/**
 * Filename: lkm_ipip_dgu.c
 * Purpose:  encapsulate outgoing packets in the special Bolouki IP-in-IP format
 * Author:   David Underhill (dgu@cs.stanford.edu) (May 2008)
 */

#include <linux/ip.h>
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/netdevice.h>
#include <linux/netfilter.h>
#include <linux/netfilter_ipv4.h>
#include <linux/skbuff.h>

#define _LKM_IPIP_DEBUG_  /* debug prints on and encap loopback traffic too */
/* #define _LKM_IPIP_DO_DECAP_ */ /* decap IP-IP and Bolouki IP-IP packets */

/** The number of bytes to put between the inner and outer IP headers. */
#define PADDING_BW_IP_HEADERS 4

/**
 * The IP protocol for the outer IP header.  It is 0xF4 for IP-IP encap, or
 * 0x04 for IP-in-IP (e.g. PADDING_BW_IP_HEADERS == 0).
 */
#if PADDING_BW_IP_HEADERS
#define LKM_IPIP_PROTO 0xF4 /* Bolouki IP-in-IP when padding separates headers */
#else
#define LKM_IPIP_PROTO 0x04 /* normal IP-in-IP when there is no padding */
#endif

/* IP addresses of destinations to do the encapsulation for */
#define IP_ADDR_HBO_HOUSTON_1  0x4039174A /* 64.57.23.74 */
#define IP_ADDR_HBO_HOUSTON_2  0x4039174B /* 64.57.23.75 */
#define IP_ADDR_HBO_LA_1       0x40391742 /* 64.57.23.66 */
#define IP_ADDR_HBO_LA_2       0x40391743 /* 64.57.23.67 */
#define IP_ADDR_HBO_NF_POWER_1 0xAB184A69 /* 172.24.74.105 */
#define IP_ADDR_HBO_NF_POWER_2 0xAB184A6A /* 172.24.74.106 */
#define IP_ADDR_HBO_LOOPBACK   0x7F000001 /* 127.0.0.1 */

#define IP_ADDR_HBO_POWER3_E1  0xC00A0101 /* 192.10.1.1 */
#define IP_ADDR_HBO_POWER4_E1  0xC00A0201 /* 192.10.2.1 */
#define IP_ADDR_HBO_POWER4_N1  0xC00A020A /* 192.10.2.10 */
#define IP_ADDR_HBO_POWER4_N0  0xC00A010A /* 192.10.1.10 */

/** who we want to address the encapsulation packet to (outer header) */
#define IP_ADDR_HBO_DECAP_TARGET IP_ADDR_HBO_POWER4_N0

static struct nf_hook_ops netfilter_encap;
#ifdef _LKM_IPIP_DO_DECAP_
static struct nf_hook_ops netfilter_decap;
#endif

/**
 * The implementation of this function is based on code in the following book:
 *  W. Richard Stevens, Bill Fenner, and Andrew M. Rudoff. UNIX Network
 *  Programming. Addison-Wesley. Volume 1, Edition 3, 2007. 753.
 */
static __u16 checksum( __u16* buf, unsigned len ) {
    __u16 answer;
    __u32 sum;

    /* add all 16 bit pairs into the total */
    answer = sum = 0;
    while( len > 1 ) {
        sum += *buf++;
        len -= 2;
    }

    /* take care of the last lone byte, if present */
    if( len == 1 ) {
        *(unsigned char *)(&answer) = *(unsigned char *)buf;
        sum += answer;
    }

    /* fold any carries back into the lower 16 bits */
    sum = (sum >> 16) + (sum & 0xFFFF);    /* add hi 16 to low 16 */
    sum += (sum >> 16);                    /* add carry           */
    answer = ~sum;                         /* truncate to 16 bits */

    return answer;
}

/**
 * Computes the transport-layer checksum for UDP and TCP packets.
 * src_ip and dst_ip are network-ordered fields, while len is a
 * host-ordered field which specifies the length of xport_packet in
 * bytes.  len must be no greater than 1502 bytes.
 *
 * Note: this could be more sophisticated and not memcpy xport_packet
 *       and just do the checksum in place
 */
uint16_t checksum_xport( __u32 src_ip, __u32 dst_ip, __u8 proto, __u8* xport_packet, __u16 len )
{
    __u8 buf[1514];
    if( unlikely(len > 1502) )
        return 0;

    /* create the pseudo-header */
    memcpy( buf,   &src_ip, sizeof(src_ip) );
    memcpy( buf+4, &dst_ip, sizeof(dst_ip) );
    buf[8] = 0x00;
    buf[9] = proto;
    *((__u16*)&buf[10]) = htons(len);

    /* copy the transport header and payload into the buffer to compute the checksum on */
    memcpy( &buf[12], xport_packet, len );
    return checksum( (__u16*)buf, 12 + len );
}

#ifdef _LKM_IPIP_DEBUG_
/**
 * Prints an IP address in dotted-decimal format with printk.
 */
static void printk_ip( __u32 ip_nbo ) {
    unsigned char* bytes;

    bytes = (unsigned char*)&ip_nbo;
    printk( "%u.%u.%u.%u", bytes[0], bytes[1], bytes[2], bytes[3] );
}
#endif

/**
 * Modify the skb to tell other modules that we changed its contents.
 */
static void skb_flag_as_changed( struct sk_buff* skb ) {
    /* Tell the netfilter framework that this packet is not the
       same as the one before! */
#ifdef CONFIG_NETFILTER
    nf_conntrack_put( skb->nfct );
    skb->nfct = NULL;
#ifdef CONFIG_NETFILTER_DEBUG
    skb->nf_debug = 0;
#endif
#endif
}

/* resize an skb */
static int skb_resize( struct sk_buff *skb, int extra_bytes ) {
    skb_orphan(skb);

    if (pskb_expand_head(skb, 0, extra_bytes, GFP_ATOMIC)) {
        printk( "*** failed to expand skb!" );
        return -1;
    }

    skb->truesize += extra_bytes;

    return 0;
}

/**
 * Adjust headroom after the data is already in place.  This means the data from
 * skb->data (inclusive) to skb->tail (exclusive) will be shifted back towards
 * skb->end.  skb->tail is updated to the new tail and skb->len is updated to
 * the new len.
 *
 * @return If skb->tail + len > skb->end, then there is not enough space left
 * and the method will do nothing and return -1.  Otherwise, 0 will be returned.
 */
static int skb_late_reserve( struct sk_buff* skb, unsigned len ) {
    unsigned i, real_data_len;
    char* from, *to;

    /* make sure we have enough space */
    if( unlikely(skb->tail + len > skb->end) ) {
#if 1
    printk( "RDROP\n" );
    return -1;
#endif
        if( skb_resize( skb, len ) != 0 )
            return -1;
        else {
#ifdef _LKM_IPIP_DEBUG_
            printk( "*** grew the skb!\n" );
            if( unlikely(skb->tail + len > skb->end) ) {
                printk( "oh crap, are we scribbling on the kernel?\n" );
                return -1;
            }
#endif
        }
    }

    /* move the data bytes (from the last byte to the first byte) */
    real_data_len = (unsigned)(skb->tail - skb->data);
    from = skb->tail;
    to = from + len;
    for( i=0; i<real_data_len; i++ )
        *(--to) = *(--from);

    /* update the length of used bytes in the skb */
    skb->tail += len;
    skb->len  += len;

    /* success */
    return 0;
}

#ifdef _LKM_IPIP_DO_DECAP_
/**
 * Removes the first skip bytes of data.  Data from skb->data + skip to
 * skb->tail is shifted up to fill in the hole.  skb->tail is updated to the new
 * tail and skb->len is updated to the new len.
 *
 * @return -1 if skip is bigger than skb->tail - skb->data (e.g. it is bigger
 * than the amount of data we have).  Otherwise, this will succeed and return 0.
 */
static int skb_remove( struct sk_buff* skb, unsigned skip ) {
    unsigned i, real_data_len;
    char* from, *to;

    /* move the data bytes (from the last byte to the first byte) */
    real_data_len = (unsigned)(skb->tail - skb->data);
    if( real_data_len < skip )
        return -1; /* can't skip more data than we have */

    from = skb->data + skip;
    to = skb->data;
    for( i=0; i<real_data_len; i++ )
        *(to++) = *(from++);

    /* update the length of used bytes in the skb */
    skb->tail -= skip;
    skb->len  -= skip;

    /* success */
    return 0;
}

unsigned int decap_hook( unsigned int hooknum,
                         struct sk_buff **pp_skb,
                         const struct net_device *in,
                         const struct net_device *out,
                         int (*okfn)(struct sk_buff*) ) {
    struct sk_buff* skb;
    unsigned extra_len;
    skb = *pp_skb;

    printk( "POSS: considering " );
    printk_ip( skb->nh.iph->saddr );
    printk( " (proto=%u)\n", skb->nh.iph->protocol );    

    /* ignore packets with no data or IP header */
    if( !skb || !skb->nh.iph )
        return NF_ACCEPT;

    /* determine if we need to do decapsulation for this target */
    if( skb->nh.iph->protocol == LKM_IPIP_PROTO ) {
        extra_len = sizeof(struct iphdr) + PADDING_BW_IP_HEADERS;

        /* make sure there are the expected # of bytes before we handle it */
        if( unlikely(skb->len < extra_len) )
            return NF_ACCEPT;

#ifdef _LKM_IPIP_DEBUG_
        printk( "LKM ENCAP: will decapsulate packet from " );
        printk_ip( skb->nh.iph->saddr );
        printk( " (proto=%u)\n", skb->nh.iph->protocol );
#endif
    }
    else {
#ifdef _LKM_IPIP_DEBUG_
        printk( "LKM ENCAP: will not decapsulate packet from " );
        printk_ip( skb->nh.iph->saddr );
        printk( " (proto=%u)\n", skb->nh.iph->protocol );
#endif
        return NF_ACCEPT;
    }

    skb_flag_as_changed( skb );

    /* overwrite the extra_len bytes at the front of skb->data */
    skb_remove( skb, extra_len );

    /* ok, give it back to the kernel */
    return NF_ACCEPT;
}
#endif

#include "debug_id.h"
static int count = 0;
void print_skb( const char* what, struct sk_buff* skb ) {
#if 0
    static int reps = 0;
    int i;
    const char* extra1 = (reps!=0 ? "" : "{");
    printk("\nDGU %s /* #%d => **%d** %s: */ char ip_%s[1500] = { ", extra1, count, DEBUG_ID, what, what);
    for( i=0; i<ntohs(skb->nh.iph->tot_len); i++ ) {
        if( i ) printk( ", " );
        printk( "0x%0X", *(((unsigned char*)skb->nh.iph)+i) );
    }
    printk(" };");
    if( reps == 2 ) {
      printk("validate(ip_before,ip_after_all+24,%d); }",count);
    }
    printk("\n");
    reps = (reps + 1) % 3;
#endif
}

unsigned int encap_hook( unsigned int hooknum,
                         struct sk_buff **pp_skb,
                         const struct net_device *in,
                         const struct net_device *out,
                         int (*okfn)(struct sk_buff*) ) {
    struct sk_buff* skb;
    struct iphdr* outer_ip_hdr;
    struct iphdr* inner_ip_hdr;
    unsigned i, extra_len;
    __u8* ptr_xport;
    __u8 proto;
    __u16* ptr_csum;
    __u16 csum_orig;
    __u16 csum_comp;
    __u16 xport_len;

    /* ignore packets with no data or IP header */
    skb = *pp_skb;
    if( !skb || !skb->nh.iph )
        return NF_ACCEPT;

    /* determine if we need to do encapsulation for this target */
    switch( ntohl(skb->nh.iph->daddr) ) {
    case IP_ADDR_HBO_HOUSTON_1:
    case IP_ADDR_HBO_HOUSTON_2:
    case IP_ADDR_HBO_LA_1:
    case IP_ADDR_HBO_LA_2:
    case IP_ADDR_HBO_NF_POWER_1:
    case IP_ADDR_HBO_NF_POWER_2:
    case IP_ADDR_HBO_POWER4_E1:
#ifdef _LKM_IPIP_DEBUG_
    case IP_ADDR_HBO_LOOPBACK:
        /* encapsulate the target ... I hope there's room in the SKB ... */
        printk( "LKM ENCAP: will encap this packet to " );
        printk_ip( skb->nh.iph->daddr );
        printk( "\n" );

	count += 1;
	print_skb( "before", skb );
#endif
        break;

    default:
#ifdef _LKM_IPIP_DEBUG_
        /* no ecapsulation needed */
        printk( "LKM ENCAP: will NOT encap this packet to " );
        printk_ip( skb->nh.iph->daddr );
        printk( "\n" );
#endif
        return NF_ACCEPT;
    }

    /* compute the checksum field for UDP/TCP packets since they otherwise aren't done yet */
    proto = skb->nh.iph->protocol;
    if( (proto==0x11 || proto==0x06) && ntohs(skb->nh.iph->tot_len)>20 ) {
        /* get a pointer to the start of the xport layer and determine its length */
        ptr_xport = ((__u8*)skb->nh.iph) + 20;
	xport_len = ntohs(skb->nh.iph->tot_len) - 20;

	/* get the original checksum and zero it so we can compute what it should be */
        ptr_csum = (__u16*)(ptr_xport + (proto==0x11 ? 6 : 16));
        csum_orig = *ptr_csum;
	*ptr_csum = 0;

	/* compute and set the final checksum */
        csum_comp = checksum_xport( skb->nh.iph->saddr, skb->nh.iph->daddr, proto, ptr_xport, xport_len );
        *ptr_csum = unlikely(csum_comp==0x00 && proto==0x11) ? 0xFF : csum_comp; /* UDP checksum 0x00 => 0xFF */
#ifdef _LKM_IPIP_DEBUG_
        if( csum_orig != csum_comp )
	    printk( "Checksum error: was 0x%0X, computed 0x%0X\n", ntohs(csum_orig), ntohs(csum_comp) );
        else
	    printk( "Checksum CORRECT" );
#endif
    }

    skb_flag_as_changed( skb );

    /* Unfortunately, we cannot call skb_reserve once data is in place ... so we
     * have to manually push the data back to make room for the header.  We will
     * reserve enough space for the header and padding all at once at least. */
    extra_len = sizeof(struct iphdr) + PADDING_BW_IP_HEADERS;
    if( skb_late_reserve( skb, extra_len ) ) {
        /* ouch, the buffer isn't big enough so just drop the packet :( */
        printk( "LKM ENCAP: Ran out of buffer space (used=%u, have=%u, need %u more)\n",
                skb->len, (unsigned)(skb->end-skb->data),
                sizeof(struct iphdr) + PADDING_BW_IP_HEADERS );
        return NF_DROP;
    }

    /* zero the padding bytes */
    for( i=sizeof(struct iphdr); i<extra_len; i++ )
        skb->nh.raw[i] = 0;

    /* push another header onto the packet */
    outer_ip_hdr = skb->nh.iph;

    /* get a pointer to the original IP header */
    inner_ip_hdr = (struct iphdr*)(((char*)(outer_ip_hdr + 1))
                                   + PADDING_BW_IP_HEADERS);

    /* fill in the external IP header ...  */
    outer_ip_hdr->version = 4;
    outer_ip_hdr->ihl = 5;
    outer_ip_hdr->tos = inner_ip_hdr->tos;
    outer_ip_hdr->tot_len = htons( skb->len );
    outer_ip_hdr->frag_off = 4;
    outer_ip_hdr->id = inner_ip_hdr->id;
    outer_ip_hdr->ttl = inner_ip_hdr->ttl;
    outer_ip_hdr->protocol = LKM_IPIP_PROTO;
    outer_ip_hdr->saddr = inner_ip_hdr->saddr;
    outer_ip_hdr->daddr = htonl( IP_ADDR_HBO_DECAP_TARGET );

    /* compute the checksum for the new IP header */
    outer_ip_hdr->check = 0;
    outer_ip_hdr->check = checksum( (__u16*)outer_ip_hdr, sizeof(struct iphdr) );
 
    print_skb( "after", skb );

    /* update the skb checksum */
    skb->csum = csum_partial( (char *)outer_ip_hdr,
                              sizeof(struct iphdr) + PADDING_BW_IP_HEADERS,
                              skb->csum);

    print_skb( "after_all", skb );

    /* ok, give it back to the kernel */
    return NF_ACCEPT;
}

int init_module() {
    printk( "LKM ENCAP: Starting the IP-IP encapsulation LKM hook\n" );

    /* tell netfilter where to give us a callback */
    netfilter_encap.hook = encap_hook;

    /* handle IPv4 packets */
    netfilter_encap.pf = PF_INET;

    /* call our hook before routing the packet */
    netfilter_encap.hooknum = NF_IP_LOCAL_OUT;

    /* call our hook before any other hook */
    netfilter_encap.priority = NF_IP_PRI_FIRST;

    /* register our new callback hook for IPv4 packets */
    nf_register_hook( &netfilter_encap );

#ifdef _LKM_IPIP_DO_DECAP_
    printk( "LKM ENCAP: Starting the IP-IP decapsulation LKM hook\n" );
    netfilter_decap.hook = decap_hook;
    netfilter_decap.pf = PF_INET;
    netfilter_decap.hooknum = NF_IP_PRE_ROUTING;
    netfilter_decap.priority = NF_IP_PRI_FIRST;
    nf_register_hook( &netfilter_decap );
#endif

    return 0;
}

void cleanup_module() {
    nf_unregister_hook(&netfilter_encap);
    printk( "LKM ENCAP: Terminated the IP-IP encapsulation LKM hook\n" );

#ifdef _LKM_IPIP_DO_DECAP_
    nf_unregister_hook(&netfilter_decap);
    printk( "LKM ENCAP: Terminated the IP-IP decapsulation LKM hook\n" );
#endif
}

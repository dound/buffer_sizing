/**
 * Filename: lkm_encap.c
 * Purpose: encapsulate outgoing packets in the special Bolouki IP-in-IP format
 */

#include <linux/ip.h>
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/netdevice.h>
#include <linux/netfilter.h>
#include <linux/netfilter_ipv4.h>
#include <linux/skbuff.h>

/** The number of bytes to put between the inner and outer IP headers. */
#define PADDING_BW_IP_HEADERS 0

/**
 * The IP protocol for the outer IP header.  It is 0xF4 for Bolouki encap, or
 * 0x04 for IP-in-IP (e.g. PADDING_BW_IP_HEADERS == 0).
 */
#if PADDING_BW_IP_HEADERS
#define ENCAP_PROTO 0xF4 /* Bolouki IP-in-IP when padding separates headers */
#else
#define ENCAP_PROTO 0x04 /* normal IP-in-IP when there is no padding */
#endif

/* IP addresses of destinations to do the encapsulation for */
#define IP_ADDR_HBO_HOUSTON_1  0x4039174A /* 64.57.23.74 */
#define IP_ADDR_HBO_HOUSTON_2  0x4039174B /* 64.57.23.75 */
#define IP_ADDR_HBO_LA_1       0x40391742 /* 64.57.23.66 */
#define IP_ADDR_HBO_LA_2       0x40391743 /* 64.57.23.67 */
#define IP_ADDR_HBO_NF_POWER_1 0xAB184A69 /* 172.24.74.105 */
#define IP_ADDR_HBO_NF_POWER_2 0xAB184A6A /* 172.24.74.106 */
#define IP_ADDR_HBO_LOOPBACK   0x7F000001 /* 127.0.0.1 */

/** who we want to address the encapsulation packet to (outer header) */
#define IP_ADDR_HBO_DECAP_TARGET IP_ADDR_HBO_HOUSTON_1

static struct nf_hook_ops netfilter_ops;

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
 * Prints an IP address in dotted-decimal format with printk.
 */
static void printk_ip( __u32 ip_nbo ) {
    unsigned char* bytes;

    bytes = (unsigned char*)&ip_nbo;
    printk( "%u.%u.%u.%u", bytes[0], bytes[1], bytes[2], bytes[3] );
}

/**
 * Adjust headroom after the data is already in place.  This means the data from
 * skb->data (inclusive) to skb->tail (exclusive) will be shifted back towards
 * skb->end.
 *
 * @return If skb->tail + len > skb->end, then there is not enough space left
 * and the method will do nothing and return -1.  Otherwise, 0 will be returned.
 */
static int skb_late_reserve( struct sk_buff* skb, unsigned len ) {
    unsigned i, real_data_len;
    char* from, *to;

    /* make sure we have enough space */
    if( unlikely(skb->tail + len > skb->end) )
        return -1;

    /* move the data bytes (from the last byte to the first byte) */
    real_data_len = (unsigned)(skb->tail - skb->data);
    from = skb->tail;
    to = from + len;
    for( i=0; i<real_data_len; i++ )
        *(--to) = *(--from);

    /* update the length of used bytes in the skb */
    skb->len += len;

    /* success */
    return 0;
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
    case IP_ADDR_HBO_LOOPBACK:
        /* encapsulate the target ... I hope there's room in the SKB ... */
        printk( "LKM ENCAP: will encap this packet to " );
        printk_ip( skb->nh.iph->daddr );
        printk( "\n" );
        break;

    default:
        /* no ecapsulation needed */
        printk( "LKM ENCAP: will NOT encap this packet to " );
        printk_ip( skb->nh.iph->daddr );
        printk( "\n" );
        return NF_ACCEPT;
    }

    /* Tell the netfilter framework that this packet is not the
       same as the one before! */
#ifdef CONFIG_NETFILTER
    nf_conntrack_put( skb->nfct );
    skb->nfct = NULL;
#ifdef CONFIG_NETFILTER_DEBUG
    skb->nf_debug = 0;
#endif
#endif

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
    outer_ip_hdr->frag_off = 0;
    outer_ip_hdr->id = inner_ip_hdr->id;
    outer_ip_hdr->ttl = inner_ip_hdr->ttl;
    outer_ip_hdr->protocol = ENCAP_PROTO;
    outer_ip_hdr->saddr = inner_ip_hdr->saddr;
    outer_ip_hdr->daddr = htonl( IP_ADDR_HBO_DECAP_TARGET );

    /* compute the checksum for the new IP header */
    outer_ip_hdr->check = 0;
    outer_ip_hdr->check = checksum( (__u16*)outer_ip_hdr, sizeof(struct iphdr) );

    /* update the skb checksum */
    skb->csum = csum_partial( (char *)outer_ip_hdr,
                              sizeof(struct iphdr) + PADDING_BW_IP_HEADERS,
                              skb->csum);

    /* ok, give the kernel back its back and hope it's ok with our changes */
    return NF_ACCEPT;
}

int init_module() {
    printk( "LKM ENCAP: Starting the Bolouki encapsulation LKM hook\n" );

    /* tell netfilter where to give us a callback */
    netfilter_ops.hook = encap_hook;

    /* handle IPv4 packets */
    netfilter_ops.pf = PF_INET;

    /* call our hook before routing the packet */
    netfilter_ops.hooknum = NF_IP_LOCAL_OUT;

    /* call our hook before any other hook */
    netfilter_ops.priority = NF_IP_PRI_FIRST;

    /* register our new callback hook for IPv4 packets */
    nf_register_hook(&netfilter_ops);

    return 0;
}

void cleanup_module() {
    nf_unregister_hook(&netfilter_ops);
    printk( "LKM ENCAP: Terminating the Bolouki encapsulation LKM hook\n" );
}

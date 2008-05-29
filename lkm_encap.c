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
#define PADDING_BETWEEN_IP_HEADERS 4

/**
 * The IP protocol for the outer IP header.  It is 0xF4 for Bolouki encap, or
 * 0x04 for IP-in-IP (e.g. PADDING_BETWEEN_IP_HEADERS == 0).
 */
#define ENCAP_PROTO 0xF4

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

static void printk_ip( __u32 ip_nbo ) {
    unsigned char* bytes;

    bytes = (unsigned char*)&ip_nbo;
    printk( "%u.%u.%u.%u", bytes[0], bytes[1], bytes[2], bytes[3] );
}

unsigned int encap_hook( unsigned int hooknum,
                        struct sk_buff **pp_skb,
                        const struct net_device *in,
                        const struct net_device *out,
                        int (*okfn)(struct sk_buff*) ) {
    struct sk_buff* skb;
    struct iphdr* outer_ip_hdr;
    struct iphdr* inner_ip_hdr;
    unsigned i;

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

        return NF_DROP;

    /* push the padding bytes into the header */
    skb->nh.raw = skb_push( skb, PADDING_BETWEEN_IP_HEADERS );

    /* zero the padding bytes */
    for( i=0; i<PADDING_BETWEEN_IP_HEADERS; i++ )
        skb->nh.raw[i] = 0;

    /* push another header onto the packet */
    skb->nh.raw = skb_push( skb, sizeof(struct iphdr) );
    outer_ip_hdr = skb->nh.iph;

    /* get a pointer to the original IP header */
    inner_ip_hdr = (struct iphdr*)(((char*)(outer_ip_hdr + 1))
                                   + PADDING_BETWEEN_IP_HEADERS);

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

    /* update the checksum */
    skb->csum = csum_partial( (char *)outer_ip_hdr,
                              sizeof(struct iphdr) + PADDING_BETWEEN_IP_HEADERS,
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

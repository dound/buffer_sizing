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

static struct nf_hook_ops netfilter_ops;

unsigned int encap_hook( unsigned int hooknum,
                        struct sk_buff **skb,
                        const struct net_device *in,
                        const struct net_device *out,
                        int (*okfn)(struct sk_buff*) ) {
    static struct sk_buff *sock_buff;

    /* drop packets from the loopback interface */
    if( strcmp(in->name,"lo") == 0 )
        return NF_DROP;

    /* ignore packets with no data or IP header */
    sock_buff = *skb;
    if( !sock_buff || !sock_buff->nh.iph )
        return NF_ACCEPT;

    return NF_ACCEPT;
}

int init_module() {
    printk( "dgu: Starting the Bolouki encapsulation LKM hook\n" );

    /* tell netfilter where to give us a callback */
    netfilter_ops.hook = encap_hook;

    /* handle IPv4 packets */
    netfilter_ops.pf = PF_INET;

    /* call our hook before routing the packet */
    netfilter_ops.hooknum = NF_IP_PRE_ROUTING;

    /* call our hook before any other hook */
    netfilter_ops.priority = NF_IP_PRI_FIRST;

    /* register our new callback hook for IPv4 packets */
    nf_register_hook(&netfilter_ops);

    return 0;
}

void cleanup_module() {
    nf_unregister_hook(&netfilter_ops);
    printk( "dgu: Terminating the Bolouki encapsulation LKM hook\n" );
}

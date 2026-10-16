# Antiscan

Antiscan blocks port scanners. It uses [Hunter](https://github.com/pebblehost/hunter) by PebbleHost and optionally
[AbuseIPDB](https://www.abuseipdb.com/) to check IPs, and it can report attempts to scan your server.
Note that this is NOT a perfect solution, and will NOT keep your server safe on its own. This is meant to be used in
conjunction with online mode and a whitelist. It also will probably not work behind a proxy like Velocity, because
every player would look like they come from the proxy's address.

Adding names/ips to the Antiscan whitelist will whitelist any connection with those names/ips - even if they don't
actually own that account or another source says they are untrustworthy. This will NOT let them bypass online mode (the
default for servers) - Minecraft's default verification is still there.

Don't share your `antiscan` folder! It will include IPs, including those that you've whitelisted (which are probably the IPs
of those you know). It will also include your AbuseIPDB key if you've added it.

## How a connection is checked

Checks never wait on the internet. Downloads and AbuseIPDB lookups run in the background, and a connection is judged on
what is already known. That means an IP AbuseIPDB hasn't been asked about yet is let in until its answer comes back; the
answer is then used (cached for an hour).

1. Local addresses (loopback and private ranges such as `192.168.x.x`) and whitelisted IPs/names always get in.
2. Blacklisted IPs/names, the Hunter list, and the AbuseIPDB blacklist are blocked.

If a download fails or is malformed, Antiscan keeps the last good copy and logs a warning. Copies older than a week are
ignored.

## Set up AbuseIPDB

If you have an AbuseIPDB key, Antiscan can check IPs with it. To enable this:

1. Start the server with Antiscan installed
2. Add your key to the newly-generated `antiscan/antiscan-key-do-not-share.txt` file (by the way, don't share that file either)
3. Restart the server

Requests are limited to 900 checks, 100 reports and 5 list downloads per UTC day so the free plan isn't used up, and the
count is saved in `antiscan/abuse-budget.json` (deleting it resets the count, not AbuseIPDB's). A 429 response pauses
requests for at least an hour.

**Reporting:** IPs are no longer reported automatically by default. Use `/antiscan report <ip>` when you have seen a port
scan yourself, or set `"auto_report": true` under `abuse_ipdb` in `antiscan2.json` to report `fail_report` matches
(the old behavior).

## Commands (v2, 1.21+)

| Command                                                         | Action                                       | Permission node                                     | Default    |
|-----------------------------------------------------------------|----------------------------------------------|-----------------------------------------------------|------------|
| `antiscan (blacklist\|whitelist) (ip\|name) list`               | Show the contents of a blacklist/whitelist.  | `antiscan.(blacklist\|whitelist).(ip\|name).list`   | Admins (3) |
| `antiscan (blacklist\|whitelist) (ip\|name) add <to_add>`       | Add something to a blacklist/whitelist.      | `antiscan.(blacklist\|whitelist).(ip\|name).add`    | Owners (4) |
| `antiscan (blacklist\|whitelist) (ip\|name) remove <to_remove>` | Remove something from a blacklist/whitelist. | `antiscan.(blacklist\|whitelist).(ip\|name).remove` | Owners (3) |
| `antiscan report <ip>`                                          | Report an IP to AbuseIPDB for port scanning. | `antiscan.report`                                   | Owners (4) |

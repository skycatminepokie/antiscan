# AntiScan

AntiScan blocks port scanners and reports them for you. Uses [Hunter](https://github.com/pebblehost/hunter) by
PebbleHost and optionally [AbuseIPDB](https://www.abuseipdb.com/) to check IPs and report attempts to scan your server.
Note that this is NOT a perfect solution, and will NOT keep your server safe on its own. This is meant to be used in
conjunction with online mode and a whitelist. It also will probably not work behind a proxy like Velocity.

Adding names/ips to the Antiscan whitelist will whitelist any connection with those names/ips - even if they don't
actually own that account or another source says they are untrustworthy. This will NOT let them bypass online mode (the
default for servers) - Minecraft's default verification is still there.

Don't share your config file! It will include IPs, including those that you've whitelisted (which are probably the IPs
of those you know).

## Set up AbuseIPDB

If you have an AbuseIPDB key, Antiscan can check IPs with it and report the IPs you've blocked (and IPs that try to log
in with your blocked names). To enable this:

1. Start the server with Antiscan installed
2. Add your key to the newly-generated `.antiscan-do-not-share` file (by the way, don't share that file either)
3. Restart the server

## Commands (v2, 1.21+)

| Command                                                         | Action                                       | Permission node                                     | Default    |
|-----------------------------------------------------------------|----------------------------------------------|-----------------------------------------------------|------------|
| `antiscan (blacklist\|whitelist) (ip\|name) list`               | Show the contents of a blacklist/whitelist.  | `antiscan.(blacklist\|whitelist).(ip\|name).list`   | Admins (3) |
| `antiscan (blacklist\|whitelist) (ip\|name) add <to_add>`       | Add something to a blacklist/whitelist.      | `antiscan.(blacklist\|whitelist).(ip\|name).add`    | Owners (4) |
| `antiscan (blacklist\|whitelist) (ip\|name) remove <to_remove>` | Remove something from a blacklist/whitelist. | `antiscan.(blacklist\|whitelist).(ip\|name).remove` | Owners (3) |
| `antiscan report <ip>`                                          | Report an IP to AbuseIPDB for port scanning. | `antiscan.report`                                   | Owners (4) |

# AntiScan

AntiScan blocks port scanners and reports them for you. Uses [Hunter](https://github.com/pebblehost/hunter) by
PebbleHost and optionally [AbuseIPDB](https://www.abuseipdb.com/) to check IPs and report attempts to scan your server.
Note that this is NOT a perfect solution, and will NOT keep your server safe on its own. This is meant to be used in
conjunction with online mode and a whitelist.

## Commands (v2, 1.21+)

| Command                                                         | Action                                       | Permission node                                     | Default    |
|-----------------------------------------------------------------|----------------------------------------------|-----------------------------------------------------|------------|
| `antiscan (blacklist\|whitelist) (ip\|name) list`               | Show the contents of a blacklist/whitelist.  | `antiscan.(blacklist\|whitelist).(ip\|name).list`   | Admins (3) |
| `antiscan (blacklist\|whitelist) (ip\|name) add <to_add>`       | Add something to a blacklist/whitelist.      | `antiscan.(blacklist\|whitelist).(ip\|name).add`    | Owners (4) |
| `antiscan (blacklist\|whitelist) (ip\|name) remove <to_remove>` | Remove something from a blacklist/whitelist. | `antiscan.(blacklist\|whitelist).(ip\|name).remove` | Owners (3) |
| `antiscan report <ip> <reason>`                                 | Report an IP to AbuseIPDB for port scanning. | `antiscan.report`                                   | Owners (4) |

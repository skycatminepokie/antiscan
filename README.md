# AntiScan

AntiScan blocks port scanners and reports them for you. Uses [Hunter](https://github.com/pebblehost/hunter) by
PebbleHost and optionally [AbuseIPDB](https://www.abuseipdb.com/) to check IPs and report attempts to scan your server.
Note that this is NOT a perfect solution, and will NOT keep your server safe on its own. This is meant to be used in
conjunction with online mode and a whitelist.

## How it works

When someone attempts to connect to your server, its IP is visible. We can check that IP against trusted databases to
decide whether to allow the connection. The **mode** is how we decide which connections are good and which are bad.
If the **mode** *matches* the connection, then it's considered bad, and we take an **action**. An **action** can be
doing nothing, dropping the connection, or *tarpitting* the connection. *Tarpitting* is trying to keep the other person
waiting as long as possible so that they have to spend more time on each server they try to connect to. This helps slow
down attackers, but it works better if more people do it. As you might imagine, this probably will make them annoyed, so
do with that information what you will.

When you add an AbuseIPDB key, AntiScan will check AbuseIPDB's blacklist about every five hours. This will blacklist
about 10,000 IPs at a time (and clear old ones, since bad IPs can stop abusing). AntiScan also gains to ability to
report bad actors when you add a key.

## Stages

There are four things that AntiScan can change right now: handshakes, logins, ping checks, and server queries.
Handshakes are how you start the login process. They happen *before* usernames are sent, so we can only check IPs at
that point. When you log in, usernames are sent. This is the default place to stop the process. Queries are what
Minecraft uses to send you server information on the Multiplayer screen. Ping checks are what shows you your ping on
the Multiplayer screen.

## Commands

| Command                                                            | Action                                                             | Permission node                                           | Default    |
|--------------------------------------------------------------------|--------------------------------------------------------------------|-----------------------------------------------------------|------------|
| `antiscan [ip/name] blacklist add <ip/name>`                       | Manually blacklist an IP/name                                      | `"antiscan.ip/name.blacklist.add"`                        | OP Level 3 |
| `antiscan [ip/name] blacklist remove <ip/name>`                    | Remove an IP/name from the manual blacklist                        | `"antiscan.ip/name.blacklist.remove"`                     | OP Level 3 |
| `antiscan [ip/name] blacklist check <ip/name>`                     | Check if an IP/name is blacklisted                                 | `"antiscan.ip/name.blacklist.check"`                      | OP Level 3 |
| `antiscan [ip/name] blacklist list`                                | List manually blacklisted IPs/names                                | `"antiscan.ip/name.blacklist.list"`                       | OP Level 3 |
| `antiscan ip blacklist list all`                                   | List all blacklisted IPs, including automatically blacklisted ones | `"antiscan.ip.blacklist.list.all"`                        | OP Level 3 |
| `antiscan ip blacklist update`                                     | Update the automatic blacklist                                     | `"antiscan.ip.blacklist.update"`                          | OP Level 4 |
| `antiscan ip blacklist update force`                               | Update the automatic blacklist, ignoring rate limits               | `"antiscan.ip.blacklist.update.force"`                    | OP Level 4 |
| `antiscan config abuseIpdbKey <key>`                               | Set the AbuseIPDB key to use                                       | `"antiscan.config.abuseIpdbKey"`                          | OP Level 4 |
| `antiscan config [handshake/login/ping/query] mode`                | Get the mode for blocking handshakes/login/pings/queries           | `"antiscan.config.handshake/login/ping/query.mode"`       | OP Level 4 |
| `antiscan config [handshake/login/ping/query] mode <mode>`         | Set the mode for blocking handshakes/login/pings/queries           | `"antiscan.config.handshake/login/ping/query.mode.set"`   | OP Level 4 |
| `antiscan config [handshake/login/ping/query] action`              | Get the action for handling blocked handshakes/login/pings/queries | `"antiscan.config.handshake/login/ping/query.action"`     | OP Level 4 |
| `antiscan config [handshake/login/ping/query] action <action>`     | Set the action for handling blocked handshakes/login/pings/queries | `"antiscan.config.handshake/login/ping/query.action.set"` | OP Level 4 |
| `antiscan config [handshake/login/ping/query] report`              | Get if blocked handshakes/login/pings/queries are reported         | `"antiscan.config.handshake/login/ping/query.report"`     | OP Level 4 |
| `antiscan config [handshake/login/ping/query] report <true/false>` | Set if blocked handshakes/login/pings/queries are reported         | `"antiscan.config.handshake/login/ping/query.report.set"` | OP Level 4 |
| `antiscan report <ip>`                                             | Send a report to AbuseIPDB for port scanning                       | `"antiscan.report"`                                       | OP Level 4 |

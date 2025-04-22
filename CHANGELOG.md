- Fix failure to launch on the second try
- Renaming tarpit to timeout
- Fix not updating blacklist when server is paused
- THIS IS A BREAKING UPDATE
  - That means that it will not work if you've installed an earlier version. To fix this, delete `config/antiscan.json`,
    `data/antiscan_ips.json`, and `data/antiscan_names.json`, then restart the game
# Microbin CLI client

Upload stuff to your bin from the CLI

## Installation and usage

### \*nix

1. Download and copy the script to somewhere on your `$PATH`
2. `chmod +x /whatever/path/mbcli`
3. Make a file `~/.config/microbin-clients/server` and paste the url to your microbin home.
4. `mbcli --help`

# Tips and Tricks

1. Create a QR Code for the uploaded paste: `mbcli -t "This is a test" | qrencode`
2. Notify on upload: `notify-send "Microbin Client" $(mbcli -t "This is a test")`
3. Delete all pasta: `./mbcli -l -lp | sed "s/pasta/remove/" | xargs -I {} curl -sL {}  > /dev/null`
4. Download all raw content into single file: `./mbcli -l -lr | xargs -I {} curl -sL {}  >> allcontent`
5. Select file and download: `curl -sLO $(./mbcli -l -lr | fzf)`


const { app, Tray, BrowserWindow, Menu } = require('electron')
const path = require('path')
const fs = require('fs')
const os = require('os')

let tray = null;
let win = null;

function createWindow() {
    win = new BrowserWindow({
        webPreferences: {
            webviewTag: true,
            preload: path.join(__dirname, 'preload.js')
        },
        show: false
    })
    win.setMenuBarVisibility(false)
    win.setAutoHideMenuBar(true)
    win.on("close", (e) => {
        win.hide()
        e.preventDefault()
    })
    win.loadFile("index.html")
    win.webContents.on("dom-ready", () => {
        fs.readFile(path.join(os.homedir(), '.config', 'microbin-clients', 'server'), 'utf8', (err, data) => {
            if (err) {
                console.error(err);
                return;
            }
            console.log("Server URL: ", data)
            win.webContents.send("url", data)
            win.show()
        });
    })
}
function createTray() {
    const iconName = 'logo.png';
    const iconPath = path.join(__dirname, "/assets/", iconName)
    tray = new Tray(iconPath);
    tray.setToolTip('AMP Notifier App');
    const contextMenu = Menu.buildFromTemplate([{
        label: "Microbin",
        click: showWin
    }, {
        label: 'Quit',
        click: destroyApp
    }]);
    tray.setContextMenu(contextMenu);
}
app.on('ready', () => {
    createWindow()
    createTray()
});

function destroyApp() {
    app.exit()
}

function showWin() {
    win.show()
}

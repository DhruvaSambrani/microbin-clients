const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('electronAPI', {
    onUrl: (callback) => ipcRenderer.on('url', callback)
})

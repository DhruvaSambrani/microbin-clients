
window.electronAPI.onUrl((event, value) => {
    console.log(value)
    let webview = document.getElementById('webview')
    webview.loadURL(value)
})

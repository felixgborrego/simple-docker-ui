const createWindowsInstaller = require('electron-winstaller').createWindowsInstaller
const path = require('path')

getInstallerConfig()
     .then(createWindowsInstaller)
     .catch((error) => {
     console.error(error.message || error)
     process.exit(1)
 })

function getInstallerConfig () {
    console.log('creating windows installer')
    const rootPath = path.join('./')
    const outPath = path.join(rootPath, 'dist')

    return Promise.resolve({
       appDirectory: path.join(outPath, 'SimpleDockerUI-win32-x64'),
       authors: 'Felix',
       name: 'SimpleDockerUI',
       title: 'SimpleDockerUI',
       noMsi: true,
       outputDirectory: path.join(outPath, 'windows-installer'),
       exe: 'SimpleDockerUI.exe',
       setupExe: 'SimpleDockerUI-setup.exe',
       setupIcon: path.join(rootPath, 'img', 'logo_small.ico')
   })
}


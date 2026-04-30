
#include <windows.h>
#include <stdio.h>

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
    char exePath[MAX_PATH];
    GetModuleFileName(NULL, exePath, MAX_PATH);

    char *lastBackslash = strrchr(exePath, '\\');
    if (lastBackslash != NULL) {
        *lastBackslash = '\0';
    }

    char batPath[MAX_PATH];
    snprintf(batPath, MAX_PATH, "%s\\bin\\sdr-trunk.bat", exePath);

    ShellExecute(NULL, "open", batPath, lpCmdLine, exePath, SW_HIDE);
    return 0;
}

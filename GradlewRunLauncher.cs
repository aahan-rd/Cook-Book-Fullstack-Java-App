using System;
using System.Diagnostics;
using System.IO;

public static class Program
{
    public static int Main(string[] args)
    {
        string repoRoot = AppDomain.CurrentDomain.BaseDirectory;
        string gradlewBat = Path.Combine(repoRoot, "gradlew.bat");

        if (!File.Exists(gradlewBat))
        {
            Console.Error.WriteLine("Could not find gradlew.bat next to this executable.");
            return 1;
        }

        var processStartInfo = new ProcessStartInfo
        {
            FileName = gradlewBat,
            Arguments = "run",
            WorkingDirectory = repoRoot,
            UseShellExecute = false
        };

        using (var process = Process.Start(processStartInfo))
        {
            process.WaitForExit();
            return process.ExitCode;
        }
    }
}

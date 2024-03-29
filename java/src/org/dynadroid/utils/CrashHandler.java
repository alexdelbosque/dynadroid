package org.dynadroid.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StatFs;
import org.dynadroid.DynaDroidActivity;

import java.io.*;
import java.util.Date;
import java.util.Random;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    String versionName;
    String packageName;
    String filePath;
    String phoneModel;
    String androidVersion;
    String board;
    String brand;
    String device;
    String display;
    String fingerPrint;
    String host;
    String id;
    String model;
    String product;
    String tags;
    long time;
    String type;
    String user;

    private Thread.UncaughtExceptionHandler previousHandler;
    private DynaDroidActivity dynaDroidActivity;

    public CrashHandler(DynaDroidActivity dynaDroidActivity) {
        this.dynaDroidActivity = dynaDroidActivity;
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        loadDeviceInfo();
    }

    public long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    void loadDeviceInfo() {
        PackageManager pm = dynaDroidActivity.getPackageManager();
        try {
            PackageInfo pi;
            // Version
            pi = pm.getPackageInfo(dynaDroidActivity.getPackageName(), 0);
            versionName = pi.versionName;
            // Package name
            packageName = pi.packageName;
            // Files dir for storing the stack traces
            filePath = dynaDroidActivity.getFilesDir().getAbsolutePath();
            // Device model
            phoneModel = android.os.Build.MODEL;
            // Android version
            androidVersion = android.os.Build.VERSION.RELEASE;

            board = android.os.Build.BOARD;
            brand = android.os.Build.BRAND;
            //CPU_ABI = android.os.Build.;
            device = android.os.Build.DEVICE;
            display = android.os.Build.DISPLAY;
            fingerPrint = android.os.Build.FINGERPRINT;
            host = android.os.Build.HOST;
            id = android.os.Build.ID;
            //Manufacturer = android.os.Build.;
            model = android.os.Build.MODEL;
            product = android.os.Build.PRODUCT;
            tags = android.os.Build.TAGS;
            time = android.os.Build.TIME;
            type = android.os.Build.TYPE;
            user = android.os.Build.USER;

        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String createInformationString() {
        StringBuffer returnVal = new StringBuffer();
        returnVal.append("Version : ").append(versionName).append("\n");
        returnVal.append("Package : ").append(packageName).append("\n");
        returnVal.append("FilePath : ").append(filePath).append("\n");
        returnVal.append("Phone Model : ").append(phoneModel).append("\n");
        returnVal.append("Android Version : ").append(androidVersion).append("\n");
        returnVal.append("Board : ").append(board).append("\n");
        returnVal.append("Brand : ").append(brand).append("\n");
        returnVal.append("Device : ").append(device).append("\n");
        returnVal.append("Display : ").append(display).append("\n");
        returnVal.append("Finger Print : ").append(fingerPrint).append("\n");
        returnVal.append("Host : ").append(host).append("\n");
        returnVal.append("ID : ").append(id).append("\n");
        returnVal.append("Model : ").append(model).append("\n");
        returnVal.append("Product : ").append(product).append("\n");
        returnVal.append("Tags : ").append(tags).append("\n");
        returnVal.append("Time : ").append(time).append("\n");
        returnVal.append("Type : ").append(type).append("\n");
        returnVal.append("User : ").append(user).append("\n");
        returnVal.append("Total Internal memory : ").append(getTotalInternalMemorySize()).append("\n");
        returnVal.append("Available Internal memory : ").append(getAvailableInternalMemorySize()).append("\n");
        return returnVal.toString();
    }

    public void uncaughtException(Thread t, Throwable e) {
        StringBuffer report = new StringBuffer();
        report.append("Error Report collected on : ").append(new Date().toString()).append("\n\n");
        report.append("Informations :\n");
        report.append("==============\n\n");
        report.append(createInformationString()).append("\n\n");
        report.append("Stack : \n");
        report.append("======= \n");
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        report.append(stacktrace).append("\n");
        report.append("Cause : \n");
        report.append("======= \n");

// If the exception was thrown in a background thread inside
// AsyncTask, then the actual exception can be found with getCause
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            report.append(result.toString());
            cause = cause.getCause();
        }
        printWriter.close();
        report.append("****  End of current Report ***");
        save(report.toString());
        previousHandler.uncaughtException(t, e);
    }

    private void save(String crashReport) {
        try {
            Random generator = new Random();
            int random = generator.nextInt(99999);
            String FileName = "stack-" + random + ".stacktrace";
            FileOutputStream trace = dynaDroidActivity.openFileOutput(FileName, Context.MODE_PRIVATE);
            trace.write(crashReport.getBytes());
            trace.close();
        }
        catch (IOException ioe) {
        }
    }

    private String[] getErrorFileList() {
        File dir = new File(filePath + "/");
        dir.mkdir();
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".stacktrace");
            }
        };
        return dir.list(filter);
    }

    public boolean hasCrashReport() {
        return getErrorFileList().length > 0;
    }

    public String getCrashReport() {
        try {
            if (hasCrashReport()) {
                StringBuffer crashReport = new StringBuffer();
                String[] ErrorFileList = getErrorFileList();
                int curIndex = 0;
                final int maxSend = 5;
                for (String curString : ErrorFileList) {
                    if (curIndex++ <= maxSend) {
                        crashReport.append("New Trace collected :\n");
                        crashReport.append("=====================\n ");
                        String filePath = this.filePath + "/" + curString;
                        BufferedReader input = new BufferedReader(new FileReader(filePath));
                        String line;
                        while ((line = input.readLine()) != null) {
                            crashReport.append(line + "\n");
                        }
                        input.close();
                    }
                    File curFile = new File(filePath + "/" + curString);
                    curFile.delete();
                }
                return crashReport.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}


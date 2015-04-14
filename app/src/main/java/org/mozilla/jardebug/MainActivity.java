package org.mozilla.jardebug;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class MainActivity extends Activity {

    private static final String LOGTAG = "JarDebug";
    private static final String DISTRIBUTION_PATH = "distribution/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.copy_jar);
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success = false;
                try {
                    success = copyJar();
                } catch (IOException e) {
                    Log.e(LOGTAG, "Error copying jar.", e);
                }
                ((TextView) findViewById(R.id.result)).setText(success ? "Copy success!" : "Copy fail!");
            }
        });
    }

    private boolean copyJar() throws IOException {
        JarInputStream distro = new JarInputStream(getResources().openRawResource(R.raw.test));

        // Try to copy distribution files from the fetched stream.
        try {
            Log.d(LOGTAG, "Copying files from fetched zip.");
            if (copyFilesFromStream(distro)) {
                return true;
            }
        } catch (SecurityException e) {
            Log.e(LOGTAG, "Security exception copying files. Corrupt or malicious?", e);
        } catch (Exception e) {
            Log.e(LOGTAG, "Error copying files from distribution.", e);
        } finally {
            distro.close();
        }
        return false;
    }

    /**
     * Unpack distribution files from a downloaded jar stream.
     *
     * The caller is responsible for closing the provided stream.
     */
    private boolean copyFilesFromStream(JarInputStream jar) throws FileNotFoundException, IOException {
        final byte[] buffer = new byte[1024];
        boolean distributionSet = false;
        JarEntry entry;
        while ((entry = jar.getNextJarEntry()) != null) {
            final String name = entry.getName();

            if (entry.isDirectory()) {
                // We'll let getDataFile deal with creating the directory hierarchy.
                // Yes, we can do better, but it can wait.
                continue;
            }

            if (!name.startsWith(DISTRIBUTION_PATH)) {
                continue;
            }

            File outFile = getDataFile(name);
            if (outFile == null) {
                continue;
            }

            distributionSet = true;

            writeStream(jar, outFile, entry.getTime(), buffer);
        }

        return distributionSet;
    }

    /**
     * Return a File instance in the data directory, ensuring
     * that the parent exists.
     *
     * @return null if the parents could not be created.
     */
    private File getDataFile(final String name) {
        File outFile = new File(getDataDir(), name);
        File dir = outFile.getParentFile();

        if (!dir.exists()) {
            Log.d(LOGTAG, "Creating " + dir.getAbsolutePath());
            if (!dir.mkdirs()) {
                Log.e(LOGTAG, "Unable to create directories: " + dir.getAbsolutePath());
                return null;
            }
        }

        return outFile;
    }

    private String getDataDir() {
        return getApplicationInfo().dataDir;
    }

    private void writeStream(InputStream fileStream, File outFile, final long modifiedTime, byte[] buffer)
            throws FileNotFoundException, IOException {
        final OutputStream outStream = new FileOutputStream(outFile);
        try {
            int count;
            while ((count = fileStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, count);
            }

            outFile.setLastModified(modifiedTime);
        } finally {
            outStream.close();
        }
    }
}

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// https://stackoverflow.com/a/15970455
public class PackZip {
    private List <String> fileList;

    public PackZip() {
        fileList = new ArrayList<>();
    }

    public static void ZipResourcePack(Path sourceFolder, Path outputZipFile) {
        PackZip appZip = new PackZip();
        appZip.generateFileList(sourceFolder.toFile(), sourceFolder.toString());
        appZip.zipIt(outputZipFile, sourceFolder);
    }

    public void zipIt(Path zipFile, Path sourceFolder) {
        byte[] buffer = new byte[1024];
        String source = sourceFolder.toString();
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile.toFile());
            zos = new ZipOutputStream(fos);
            FileInputStream in = null;

            for (String file: this.fileList) {
                ZipEntry ze = new ZipEntry(file);

                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(source + File.separator + file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } finally {
                    in.close();
                }
            }

            zos.closeEntry();
            System.out.println("Folder successfully compressed");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void generateFileList(File node, String source) {
        // add file only
        if (node.isFile()) {
            fileList.add(generateZipEntry(node.toString(), source));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename: subNote) {
                generateFileList(new File(node, filename), source);
            }
        }
    }

    private String generateZipEntry(String file, String sourceFolder) {
        return file.substring(sourceFolder.length() + 1);
    }
}

package com.cpterminal; // Sesuaikan dengan package kamu

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;

public class MyDocumentsProvider extends DocumentsProvider {

    // Sesuaikan Authority dengan yang ada di AndroidManifest
    public static final String AUTHORITY = "com.cpterminal.documents";

    // Kolom standar yang diperlukan sistem
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
    };

    @Override
    public boolean onCreate() {
        return true;
    }

@Override
public Cursor queryRoots(String[] projection) {
    MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);

    String rootPath = getContext().getFilesDir().getAbsolutePath();
    
    MatrixCursor.RowBuilder row = result.newRow();
    row.add(DocumentsContract.Root.COLUMN_ROOT_ID, "main_root");
    row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, rootPath);
    row.add(DocumentsContract.Root.COLUMN_TITLE, "cPterminal");
    row.add(DocumentsContract.Root.COLUMN_SUMMARY, "Internal Storage"); // Menambah keterangan di bawah nama
    
    // FLAGS PENTING agar muncul di sidebar
    row.add(DocumentsContract.Root.COLUMN_FLAGS, 
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE | 
            DocumentsContract.Root.FLAG_LOCAL_ONLY | 
            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);
            
    row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);

    // Tambahkan info kapasitas (opsional, tapi membantu sistem mendeteksi root valid)
    row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, getContext().getFilesDir().getFreeSpace());

    return result;
}


@Override
public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
    MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
    
    File file = new File(documentId);
    
    // Jika path tidak ditemukan, arahkan ke internal files aplikasi
    if (!file.exists()) {
        file = getContext().getFilesDir();
    }
    
    includeFile(result, file.getAbsolutePath(), file);
    return result;
}


@Override
public boolean isChildDocument(String parentDocumentId, String documentId) {
    return documentId.startsWith(parentDocumentId);
}

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        File parent = new File(parentDocumentId);
        
        if (parent.exists() && parent.isDirectory()) {
            for (File file : parent.listFiles()) {
                includeFile(result, file.getAbsolutePath(), file);
            }
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        File file = new File(documentId);
        int accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(file, accessMode);
    }

    // Fungsi pembantu untuk memasukkan data file ke dalam Cursor
    private void includeFile(MatrixCursor result, String docId, File file) {
    MatrixCursor.RowBuilder row = result.newRow();
    // Gunakan Absolute Path sebagai ID Dokumen
    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.getAbsolutePath());
    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.getName());
    row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified());
    
    if (file.isDirectory()) {
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR);
        // Tambahkan Flag agar folder bisa diklik
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 
                DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE | 
                DocumentsContract.Document.FLAG_SUPPORTS_WRITE);
    } else {
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getMimeType(file));
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 
                DocumentsContract.Document.FLAG_SUPPORTS_WRITE | 
                DocumentsContract.Document.FLAG_SUPPORTS_DELETE);
    }
    row.add(DocumentsContract.Document.COLUMN_SIZE, file.length());
}


    private String getMimeType(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            String extension = name.substring(lastDot + 1);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }
}

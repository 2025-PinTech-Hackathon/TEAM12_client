package com.example.kyc_kakaohackaton;

import android.net.Uri;

public class DocumentItem {
    private String documentName;
    private Uri fileUri;

    public DocumentItem(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public void setFileUri(Uri uri) {
        this.fileUri = uri;
    }
}


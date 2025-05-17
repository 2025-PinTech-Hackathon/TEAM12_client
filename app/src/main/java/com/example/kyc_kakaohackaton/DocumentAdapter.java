package com.example.kyc_kakaohackaton;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.view.*;
import android.widget.*;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DocumentAdapter extends BaseAdapter {

    private final Context context;
    private final List<DocumentItem> documentList;
    private final OnCameraClickListener cameraClickListener;
    private final OnUploadClickListener uploadClickListener;

    private Uri tempUri;  // 카메라 촬영을 위한 임시 URI

    public interface OnCameraClickListener {
        void onCameraClick(int position);
    }

    public interface OnUploadClickListener {
        void onUploadClick(int position);
    }

    public DocumentAdapter(Context context, List<DocumentItem> documentList,
                           OnCameraClickListener cameraClickListener,
                           OnUploadClickListener uploadClickListener) {
        this.context = context;
        this.documentList = documentList;
        this.cameraClickListener = cameraClickListener;
        this.uploadClickListener = uploadClickListener;
    }

    @Override
    public int getCount() {
        return documentList.size();
    }

    @Override
    public Object getItem(int position) {
        return documentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // 임시 사진 파일 생성 및 FileProvider로 Uri 얻기
    public void prepareTempUri() {
        try {
            File photoFile = createImageFile();
            tempUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider",
                    photoFile);
        } catch (IOException e) {
            e.printStackTrace();
            tempUri = null;
        }
    }

    public Uri getTempUri() {
        return tempUri;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "KYC_" + timeStamp;
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DocumentItem item = (DocumentItem) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        }

        TextView documentName = convertView.findViewById(R.id.text_document_name);
        ImageButton cameraButton = convertView.findViewById(R.id.button_camera);
        ImageButton uploadButton = convertView.findViewById(R.id.button_upload);
        TextView status = convertView.findViewById(R.id.text_status);

        documentName.setText(item.getDocumentName());

        cameraButton.setOnClickListener(v -> {
            if (cameraClickListener != null) {
                cameraClickListener.onCameraClick(position);
            }
        });

        uploadButton.setOnClickListener(v -> {
            if (uploadClickListener != null) {
                uploadClickListener.onUploadClick(position);
            }
        });

        // 상태 표시
        if (item.getFileUri() != null) {
            status.setText("✅ 업로드됨");
        } else {
            status.setText("❌ 미제출");
        }

        return convertView;
    }
}

package com.example.kyc_kakaohackaton;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.*;

public class MainActivity2 extends AppCompatActivity {

    private ListView documentListView;
    private DocumentAdapter adapter;
    private List<DocumentItem> documentItems;
    private int selectedPosition = -1;

    private final OkHttpClient client = new OkHttpClient();

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result && selectedPosition != -1) {
                    DocumentItem item = documentItems.get(selectedPosition);
                    item.setFileUri(adapter.getTempUri());
                    adapter.notifyDataSetChanged();
                }
            });

    private final ActivityResultLauncher<String> fileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && selectedPosition != -1) {
                    DocumentItem item = documentItems.get(selectedPosition);
                    item.setFileUri(uri);
                    adapter.notifyDataSetChanged();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // 카메라 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        // 문서 리스트 초기화
        documentListView = findViewById(R.id.listView);
        documentItems = new ArrayList<>();
        documentItems.add(new DocumentItem("사업자등록증"));
        documentItems.add(new DocumentItem("법인등기부등본"));
        documentItems.add(new DocumentItem("법인인감증명서"));
        documentItems.add(new DocumentItem("주주명부"));
        documentItems.add(new DocumentItem("대표자 신분증"));
        documentItems.add(new DocumentItem("대리인 신분증"));

        adapter = new DocumentAdapter(this, documentItems,
                pos -> {
                    selectedPosition = pos;
                    adapter.prepareTempUri();
                    cameraLauncher.launch(adapter.getTempUri());
                },
                pos -> {
                    selectedPosition = pos;
                    fileLauncher.launch("*/*");
                });

        documentListView.setAdapter(adapter);

        Button sendButton = findViewById(R.id.button_send);
        sendButton.setOnClickListener(v -> {
            boolean allFilled = true;
            for (DocumentItem item : documentItems) {
                if (item.getFileUri() == null) {
                    allFilled = false;
                    break;
                }
            }

            if (!allFilled) {
                Toast.makeText(this, "모든 문서에 파일을 첨부해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 로그인 요청
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            String jsonBody = "{\"username\":\"test\"}";
            RequestBody loginBody = RequestBody.create(jsonBody, JSON);

            Request loginRequest = new Request.Builder()
                    .url("http://172.100.2.236/login")
                    .post(loginBody)
                    .build();

            client.newCall(loginRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity2.this, "로그인 실패", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        final String errorBody = response.body() != null ? response.body().string() : "No response body";
                        runOnUiThread(() -> Toast.makeText(MainActivity2.this, "로그인 실패: " + errorBody, Toast.LENGTH_LONG).show());
                        return;
                    }

                    String token = response.body().string().replace("\"", "").trim();

                    // 순차적으로 업로드 시작
                    uploadImagesSequentially(documentItems, 0, token);
                }
            });
        });
    }

    // 이미지 파일을 하나씩 순차적으로 업로드하는 메서드
    private void uploadImagesSequentially(List<DocumentItem> items, int index, String token) {
        if (index >= items.size()) {
            runOnUiThread(() -> Toast.makeText(MainActivity2.this, "모든 파일 업로드 완료", Toast.LENGTH_SHORT).show());
            return;
        }

        DocumentItem item = items.get(index);
        Uri fileUri = item.getFileUri();

        File file;
        try {
            file = getFileFromUri(this, fileUri);
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(MainActivity2.this, "파일 처리 중 오류 발생", Toast.LENGTH_SHORT).show());
            return;
        }

        if (file == null || !file.exists()) {
            runOnUiThread(() -> Toast.makeText(MainActivity2.this, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show());
            return;
        }

        String mimeType = getContentResolver().getType(fileUri);
        if (mimeType == null) mimeType = "image/jpeg"; // 기본값

        RequestBody fileBody = RequestBody.create(file, MediaType.parse(mimeType));
        MultipartBody uploadBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), fileBody)
                .build();

        Request uploadRequest = new Request.Builder()
                .url("http://172.100.2.236/analyze")
                .addHeader("Authorization", "Bearer " + token)
                .post(uploadBody)
                .build();

        client.newCall(uploadRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity2.this, "파일 업로드 실패", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String error = response.body() != null ? response.body().string() : "에러 응답 없음";
                    System.out.println("업로드 실패: " + error);
                } else {
                    System.out.println("업로드 성공: " + file.getName());
                }

                // 다음 문서 업로드
                uploadImagesSequentially(items, index + 1, token);
            }
        });
    }

    // Uri로부터 파일을 복사해 File로 저장하는 메서드
    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        String fileName = "temp_" + System.currentTimeMillis();
        File tempFile = new File(context.getCacheDir(), fileName);

        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream == null) {
                throw new IOException("InputStream is null for Uri: " + uri);
            }

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }
}













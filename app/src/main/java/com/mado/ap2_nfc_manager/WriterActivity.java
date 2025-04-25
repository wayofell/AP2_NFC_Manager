package com.mado.ap2_nfc_manager;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;

public class WriterActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private EditText etMessage;
    private TextView tvTagContent;
    private Button btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writer);

        etMessage = findViewById(R.id.etMessage);
        tvTagContent = findViewById(R.id.tvTagContent);
        btnExit = findViewById(R.id.btnExit);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {
            String message = etMessage.getText().toString();

            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message to write", Toast.LENGTH_SHORT).show();
                return;
            }

            writeToTag(tag, message);
        }
    }

    private void writeToTag(Tag tag, String message) {
        try {
            Ndef ndef = Ndef.get(tag);

            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is read-only", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }

                NdefMessage ndefMessage = createNdefMessage(message);

                int size = ndefMessage.toByteArray().length;
                if (ndef.getMaxSize() < size) {
                    Toast.makeText(this, "Tag doesn't have enough space. Message size: " + size + " bytes, Tag size: " + ndef.getMaxSize() + " bytes", Toast.LENGTH_LONG).show();
                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                Toast.makeText(this, "Message written successfully", Toast.LENGTH_SHORT).show();
                tvTagContent.setText("Message written: " + message);

                ndef.close();

            } else {
                NdefFormatable format = NdefFormatable.get(tag);

                if (format != null) {
                    try {
                        format.connect();
                        NdefMessage ndefMessage = createNdefMessage(message);
                        format.format(ndefMessage);
                        Toast.makeText(this, "Tag formatted and message written", Toast.LENGTH_SHORT).show();
                        tvTagContent.setText("Message written: " + message);
                        format.close();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to format tag: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Tag doesn't support NDEF", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(this, "Write operation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private NdefMessage createNdefMessage(String content) {
        NdefRecord record = NdefRecord.createMime("text/plain", content.getBytes(Charset.forName("UTF-8")));

        return new NdefMessage(new NdefRecord[] {record});
    }
}
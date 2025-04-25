package com.mado.ap2_nfc_manager;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class ReaderActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView tvTagId;
    private TextView tvTagMessage;
    private Button btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        tvTagId = findViewById(R.id.tvTagId);
        tvTagMessage = findViewById(R.id.tvTagMessage);
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

        processIntent(getIntent());
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
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                byte[] tagId = tag.getId();
                tvTagId.setText("Tag ID: " + bytesToHexString(tagId));

                Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (rawMsgs != null) {
                    NdefMessage[] messages = new NdefMessage[rawMsgs.length];
                    for (int i = 0; i < rawMsgs.length; i++) {
                        messages[i] = (NdefMessage) rawMsgs[i];
                    }

                    if (messages.length > 0) {
                        NdefRecord[] records = messages[0].getRecords();
                        if (records.length > 0) {
                            NdefRecord record = records[0];
                            String payload = parseNdefRecord(record);
                            tvTagMessage.setText("Message: \n" + payload);
                        }
                    }
                } else {
                    tvTagMessage.setText("No NDEF message found");
                }
            }
        }
    }

    private String parseNdefRecord(NdefRecord record) {
        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                java.util.Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
            try {
                byte[] payload = record.getPayload();
                String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
                int languageCodeLength = payload[0] & 0x3F;
                return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            } catch (UnsupportedEncodingException e) {
                return "Error parsing NDEF record";
            }
        } else if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
                new String(record.getType()).equals("text/plain")) {
            try {
                byte[] payload = record.getPayload();
                return new String(payload, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return "Error parsing NDEF record";
            }
        }

        byte[] payload = record.getPayload();
        try {
            return new String(payload, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return bytesToHexString(payload);
        }
    }

    private String bytesToHexString(byte[] bytes) {
        final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b & 0xF0) >> 4])
                    .append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }
}
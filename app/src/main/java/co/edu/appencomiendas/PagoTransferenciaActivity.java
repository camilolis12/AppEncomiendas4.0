package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.ClipData;
import android.content.ClipboardManager;

public class PagoTransferenciaActivity extends AppCompatActivity {

    private String monto;
    private Button btnConfirmarTransferencia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pago_transferencia);

        monto = getIntent().getStringExtra("monto");

        TextView txtMonto = findViewById(R.id.txtMonto);
        TextView txtBanco = findViewById(R.id.txtBanco);
        TextView txtNumeroCuenta = findViewById(R.id.txtNumeroCuenta);
        TextView txtTitular = findViewById(R.id.txtTitular);
        Button btnCopiarCuenta = findViewById(R.id.btnCopiarCuenta);
        btnConfirmarTransferencia = findViewById(R.id.btnConfirmarTransferencia);

        txtMonto.setText("Monto a transferir: " + monto + " COP");
        txtBanco.setText("Banco: Banco Ficticio Colombia");
        txtNumeroCuenta.setText("Cuenta: 123456789");
        txtTitular.setText("Titular: EncomiendasApp S.A.S");

        btnCopiarCuenta.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Cuenta", "123456789");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Número de cuenta copiado", Toast.LENGTH_SHORT).show();
        });

        btnConfirmarTransferencia.setOnClickListener(v -> confirmarTransferencia());
    }

    private void confirmarTransferencia() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("✅ Transferencia registrada")
                .setMessage("Hemos recibido tu solicitud de transferencia por " + monto + " COP.\n\nEl pago será procesado en 24 horas.\nNúmero de referencia: REF" + System.currentTimeMillis())
                .setPositiveButton("OK", (d, w) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}

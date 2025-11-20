package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import com.google.android.material.textfield.TextInputEditText;
import android.app.AlertDialog;

public class PagoTarjetaActivity extends AppCompatActivity {

    private TextInputEditText edtNumTarjeta, edtNombreTitular, edtFechaVencimiento, edtCVV;
    private Button btnProcesarPago;
    private String monto;
    private AlertDialog procesandoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pago_tarjeta);

        monto = getIntent().getStringExtra("monto");

        edtNumTarjeta = findViewById(R.id.edtNumTarjeta);
        edtNombreTitular = findViewById(R.id.edtNombreTitular);
        edtFechaVencimiento = findViewById(R.id.edtFechaVencimiento);
        edtCVV = findViewById(R.id.edtCVV);
        btnProcesarPago = findViewById(R.id.btnProcesarPago);

        TextView txtMonto = findViewById(R.id.txtMonto);
        txtMonto.setText("Monto a pagar: " + monto + " COP");

        btnProcesarPago.setOnClickListener(v -> procesarPago());
    }

    private void procesarPago() {
        String numTarjeta = edtNumTarjeta.getText().toString().trim();
        String nombre = edtNombreTitular.getText().toString().trim();
        String fecha = edtFechaVencimiento.getText().toString().trim();
        String cvv = edtCVV.getText().toString().trim();

        if (numTarjeta.isEmpty() || nombre.isEmpty() || fecha.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (numTarjeta.length() < 13) {
            Toast.makeText(this, "Número de tarjeta inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simular procesamiento sin AlertDialog
        btnProcesarPago.setEnabled(false);
        btnProcesarPago.setText("Procesando...");

        new android.os.Handler().postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            new AlertDialog.Builder(PagoTarjetaActivity.this)
                    .setTitle("✅ Pago exitoso")
                    .setMessage("Tu pago de " + monto + " COP ha sido procesado.\n\nNúmero de transacción: TRX" + System.currentTimeMillis())
                    .setPositiveButton("OK", (d, w) -> {
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }, 2000);
    }
}

package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;

public class PagoEfectivoActivity extends AppCompatActivity {

    private String monto;
    private Button btnConfirmarEfectivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pago_efectivo);

        monto = getIntent().getStringExtra("monto");

        TextView txtMonto = findViewById(R.id.txtMonto);
        TextView txtInstrucciones = findViewById(R.id.txtInstrucciones);
        btnConfirmarEfectivo = findViewById(R.id.btnConfirmarEfectivo);

        txtMonto.setText("Monto a pagar en efectivo: " + monto + " COP");
        txtInstrucciones.setText("📍 Instrucciones:\n\n" +
                "1. Dirígete a nuestra oficina más cercana\n" +
                "2. Comunica el radicado de tu paquete\n" +
                "3. Realiza el pago en efectivo\n" +
                "4. Recibe el comprobante\n\n" +
                "Oficinas abiertas de 8am a 6pm de lunes a viernes.");

        btnConfirmarEfectivo.setOnClickListener(v -> confirmarEfectivo());
    }

    private void confirmarEfectivo() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("✅ Pago en efectivo registrado")
                .setMessage("Tu solicitud de pago en efectivo ha sido registrada.\n\nDebes completar el pago en nuestras oficinas dentro de 48 horas.\n\nNúmero de transacción: EFE" + System.currentTimeMillis())
                .setPositiveButton("OK", (d, w) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}

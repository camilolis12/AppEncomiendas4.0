package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ServicioActivity extends AppCompatActivity {

    private EditText inputRadicado, inputMensaje;
    private Button btnEnviarSoporte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicio);

        inputRadicado = findViewById(R.id.inputRadicado);
        inputMensaje = findViewById(R.id.inputMensaje);
        btnEnviarSoporte = findViewById(R.id.btnEnviarSoporte);

        btnEnviarSoporte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String radicado = inputRadicado.getText().toString().trim();
                String mensaje = inputMensaje.getText().toString().trim();

                if (radicado.isEmpty()) {
                    Toast.makeText(ServicioActivity.this, "Por favor ingresa el número de radicado", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mensaje.isEmpty()) {
                    mensaje = "Hola, quiero consultar el estado de mi paquete con radicado: " + radicado;
                } else {
                    mensaje = "Radicado: " + radicado + "\n" + mensaje;
                }

                // Intent WhatsApp
                try {
                    String url = "https://api.whatsapp.com/send?phone=+573001112233&text=" +
                            java.net.URLEncoder.encode(mensaje, "UTF-8");
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (Exception e) {
                    // Si no hay WhatsApp, abre correo
                    Intent email = new Intent(Intent.ACTION_SEND);
                    email.setType("message/rfc822");
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{"soporte@encomiendas.com"});
                    email.putExtra(Intent.EXTRA_SUBJECT, "Consulta paquete - Radicado " + radicado);
                    email.putExtra(Intent.EXTRA_TEXT, mensaje);
                    startActivity(Intent.createChooser(email, "Enviar correo con..."));
                }
            }
        });
    }
}

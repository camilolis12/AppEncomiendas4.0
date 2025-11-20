package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.*;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.AlertDialog;

public class FormularioActivity extends AppCompatActivity {

    // Remitente
    TextInputEditText edtRemitenteNombre, edtRemitenteTelefono, edtRemitenteDireccion;
    // Destinatario
    TextInputEditText edtDestinatarioNombre, edtDestinatarioTelefono, edtDestinatarioDireccion;
    // Paquete
    Spinner spinnerTamano;
    TextInputEditText edtPeso, edtLongitud;
    RadioGroup radioGroupPago;
    Button btnRegistrar, btnVerFactura, btnCompartir, btnUsarFavoritos;
    TextView txtRadicadoGenerado;
    Switch switchExpress;
    TextView txtTarifaExpress;

    private static final int PERMISO_ESCRITURA = 1;
    private static final int REQ_SEND_SMS = 3;
    private static final int REQUEST_PAGO = 100;
    private File archivoPDFGenerado; // referencia al último PDF creado
    private static final String EXPRESS_TARIFA_TEXT = "$25.000 COP"; // tarifa fija ejemplo
    String usuarioActual;
    private String pendingSmsNumber = null;
    private String pendingSmsBody = null;
    private String metodoPagoSeleccionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario);

        txtRadicadoGenerado = findViewById(R.id.txtRadicadoGenerado);
        btnVerFactura = findViewById(R.id.btnVerFactura);
        btnVerFactura.setVisibility(View.GONE);

        // Enlazar vistas
        edtRemitenteNombre = findViewById(R.id.edtRemitenteNombre);
        edtRemitenteTelefono = findViewById(R.id.edtRemitenteTelefono);
        edtRemitenteDireccion = findViewById(R.id.edtRemitenteDireccion);

        edtDestinatarioNombre = findViewById(R.id.edtDestinatarioNombre);
        edtDestinatarioTelefono = findViewById(R.id.edtDestinatarioTelefono);
        edtDestinatarioDireccion = findViewById(R.id.edtDestinatarioDireccion);

        spinnerTamano = findViewById(R.id.spinnerTamano);
        edtPeso = findViewById(R.id.edtPeso);
        edtLongitud = findViewById(R.id.edtLongitud);

        switchExpress = findViewById(R.id.switchExpress);
        txtTarifaExpress = findViewById(R.id.txtTarifaExpress);

        // Preseleccionar si viene desde SeccionActivity (botón Express)
        boolean abrirExpress = getIntent().getBooleanExtra("express", false);
        if (abrirExpress) {
            switchExpress.setChecked(true);
            txtTarifaExpress.setVisibility(View.VISIBLE);
            txtTarifaExpress.setText("Tarifa: " + EXPRESS_TARIFA_TEXT);
        }

        // Mostrar/ocultar tarifa al cambiar switch
        switchExpress.setOnCheckedChangeListener((buttonView, isChecked) -> {
            txtTarifaExpress.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) txtTarifaExpress.setText("Tarifa: " + EXPRESS_TARIFA_TEXT);
        });

        radioGroupPago = findViewById(R.id.radioGroupPago);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnCompartir = findViewById(R.id.btnCompartir);
        btnUsarFavoritos = findViewById(R.id.btnUsarFavoritos);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbarFormulario);
        setSupportActionBar(toolbar);

        btnCompartir.setOnClickListener(v -> compartirSolicitud());

        btnRegistrar.setOnClickListener(v -> registrarEnvio());
        btnVerFactura.setOnClickListener(v -> abrirFacturaPDF());
        btnUsarFavoritos.setOnClickListener(v -> mostrarFavoritos());

        // Solicitar permiso de escritura
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISO_ESCRITURA);
        }

        usuarioActual = getIntent().getStringExtra("usuario"); // puede ser null

        // Autocompletar remitente si usuarioActual existe
        if (usuarioActual != null && !usuarioActual.isEmpty()) {
            DBHelper dbHelper = new DBHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT nombreCompleto, telefono, direccion FROM " + DBHelper.TABLE_USUARIOS + " WHERE usuario = ?", new String[]{usuarioActual});
            if (c.moveToFirst()) {
                edtRemitenteNombre.setText(c.getString(0));
                edtRemitenteTelefono.setText(c.getString(1));
                edtRemitenteDireccion.setText(c.getString(2));
            }
            c.close();
            db.close();
        }
    }

    private void registrarEnvio() {
        String remitenteNombre = edtRemitenteNombre.getText().toString();
        String remitenteTelefono = edtRemitenteTelefono.getText().toString();
        String remitenteDireccion = edtRemitenteDireccion.getText().toString();

        String destinatarioNombre = edtDestinatarioNombre.getText().toString();
        String destinatarioTelefono = edtDestinatarioTelefono.getText().toString();
        String destinatarioDireccion = edtDestinatarioDireccion.getText().toString();

        String tamano = spinnerTamano.getSelectedItem() != null ? spinnerTamano.getSelectedItem().toString().trim().toLowerCase() : "";
        String peso = edtPeso.getText().toString();
        String longitud = edtLongitud.getText().toString();

        int selectedPago = radioGroupPago.getCheckedRadioButtonId();
        if (selectedPago == -1) {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton radioButton = findViewById(selectedPago);
        metodoPagoSeleccionado = (radioButton != null) ? radioButton.getText().toString() : "No seleccionado";

        // Validar campos obligatorios
        if (remitenteNombre.isEmpty() || remitenteTelefono.isEmpty() || remitenteDireccion.isEmpty() ||
                destinatarioNombre.isEmpty() || destinatarioTelefono.isEmpty() || destinatarioDireccion.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generar número de radicado
        String radicado = "ENC" + System.currentTimeMillis();

        // Calcular tarifa según tamaño
        int tarifaBase = 0;
        if ("pequeño".equals(tamano)) {
            tarifaBase = 15000;
        } else if ("mediano".equals(tamano)) {
            tarifaBase = 30000;
        } else if ("grande".equals(tamano)) {
            tarifaBase = 60000;
        } else {
            tarifaBase = 0;
        }

        // Express
        boolean isExpress = switchExpress != null && switchExpress.isChecked();
        int expressFlag = isExpress ? 1 : 0;
        int tarifaTotal = tarifaBase + (isExpress ? 25000 : 0);
        String tarifaText = String.format("%,d", tarifaTotal);

        // Mostrar resumen de costos
        new AlertDialog.Builder(this)
                .setTitle("Resumen de Costos")
                .setMessage("Tarifa base: $" + String.format("%,d", tarifaBase) + " COP\n" +
                        (isExpress ? "Cargo Express: $25,000 COP\n" : "") +
                        "Total: $" + tarifaText + " COP\n\n" +
                        "Método de pago: " + metodoPagoSeleccionado)
                .setPositiveButton("Proceder al pago", (dialog, which) -> {
                    abrirPasarelaPago(metodoPagoSeleccionado, tarifaText);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirPasarelaPago(String metodoPago, String monto) {
        Intent intent;

        if ("Tarjeta de crédito".equals(metodoPago)) {
            intent = new Intent(this, PagoTarjetaActivity.class);
        } else if ("Transferencia bancaria".equals(metodoPago)) {
            intent = new Intent(this, PagoTransferenciaActivity.class);
        } else if ("Efectivo".equals(metodoPago)) {
            intent = new Intent(this, PagoEfectivoActivity.class);
        } else {
            Toast.makeText(this, "Método de pago no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        intent.putExtra("monto", monto);
        startActivityForResult(intent, REQUEST_PAGO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PAGO && resultCode == RESULT_OK) {
            // Después de pago exitoso, continuar con registro
            continueAfterPayment();
        }
    }

    private void continueAfterPayment() {
        String remitenteNombre = edtRemitenteNombre.getText().toString();
        String remitenteTelefono = edtRemitenteTelefono.getText().toString();
        String remitenteDireccion = edtRemitenteDireccion.getText().toString();
        String destinatarioNombre = edtDestinatarioNombre.getText().toString();
        String destinatarioTelefono = edtDestinatarioTelefono.getText().toString();
        String destinatarioDireccion = edtDestinatarioDireccion.getText().toString();
        String tamano = spinnerTamano.getSelectedItem() != null ? spinnerTamano.getSelectedItem().toString() : "";
        String peso = edtPeso.getText().toString();
        String longitud = edtLongitud.getText().toString();

        String radicado = "ENC" + System.currentTimeMillis();

        int tarifaBase = 0;
        if ("pequeño".equals(tamano)) { tarifaBase = 15000; }
        else if ("mediano".equals(tamano)) { tarifaBase = 30000; }
        else if ("grande".equals(tamano)) { tarifaBase = 60000; }

        boolean isExpress = switchExpress != null && switchExpress.isChecked();
        int expressFlag = isExpress ? 1 : 0;
        int tarifaTotal = tarifaBase + (isExpress ? 25000 : 0);
        String tarifaText = String.format("%,d", tarifaTotal);

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("INSERT INTO " + DBHelper.TABLE_PAQUETES +
                        " (radicado, remitenteNombre, remitenteTelefono, remitenteDireccion, " +
                        "destinatarioNombre, destinatarioTelefono, destinatarioDireccion, " +
                        "tamano, peso, longitud, metodoPago, express, tarifa) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{radicado, remitenteNombre, remitenteTelefono, remitenteDireccion,
                        destinatarioNombre, destinatarioTelefono, destinatarioDireccion,
                        tamano, peso, longitud, metodoPagoSeleccionado, expressFlag, tarifaText});
        db.close();

        continueWithRegistration(radicado, remitenteNombre, remitenteTelefono, remitenteDireccion,
            destinatarioNombre, destinatarioTelefono, destinatarioDireccion,
            tamano, peso, longitud, metodoPagoSeleccionado, isExpress);
    }

    // Actualiza la firma para recibir todos los datos
    private void continueWithRegistration(String radicado, String remitenteNombre, String remitenteTelefono, String remitenteDireccion,
                                          String destinatarioNombre, String destinatarioTelefono, String destinatarioDireccion,
                                          String tamano, String peso, String longitud, String metodoPago, boolean isExpress) {
        txtRadicadoGenerado.setText("Número de radicado: " + radicado);
        txtRadicadoGenerado.setVisibility(View.VISIBLE);

        txtRadicadoGenerado.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Radicado", radicado);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Radicado copiado al portapapeles", Toast.LENGTH_SHORT).show();
        });

        // Pasa todos los datos a generarFacturaPDF
        archivoPDFGenerado = generarFacturaPDF(radicado, remitenteNombre, remitenteTelefono, remitenteDireccion,
                destinatarioNombre, destinatarioTelefono, destinatarioDireccion,
                tamano, peso, longitud, metodoPago, isExpress);

        if (archivoPDFGenerado != null) {
            btnVerFactura.setVisibility(View.VISIBLE);
        }

        if (usuarioActual != null && !usuarioActual.isEmpty()) {
            enviarAlertasUsuario(usuarioActual, radicado);
        }
    }

    // Cambia la firma de generarFacturaPDF para recibir isExpress
    private File generarFacturaPDF(String radicado, String rNombre, String rTelefono, String rDireccion,
                                   String dNombre, String dTelefono, String dDireccion,
                                   String tamano, String peso, String longitud, String metodoPago, boolean isExpress) {

        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        int y = 30;
        paint.setFakeBoldText(true);
        canvas.drawText("FACTURA DE ENVÍO", 80, y, paint);
        paint.setFakeBoldText(false);

        y += 30;
        canvas.drawText("Radicado: " + radicado, 10, y, paint);
        y += 20;
        canvas.drawText("Fecha: " + java.time.LocalDate.now(), 10, y, paint);

        y += 40;
        paint.setFakeBoldText(true);
        canvas.drawText("Remitente", 10, y, paint);
        paint.setFakeBoldText(false);
        y += 20;
        canvas.drawText("Nombre: " + rNombre, 10, y, paint);
        y += 20;
        canvas.drawText("Teléfono: " + rTelefono, 10, y, paint);
        y += 20;
        canvas.drawText("Dirección: " + rDireccion, 10, y, paint);

        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("Destinatario", 10, y, paint);
        paint.setFakeBoldText(false);
        y += 20;
        canvas.drawText("Nombre: " + dNombre, 10, y, paint);
        y += 20;
        canvas.drawText("Teléfono: " + dTelefono, 10, y, paint);
        y += 20;
        canvas.drawText("Dirección: " + dDireccion, 10, y, paint);

        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("Detalles del paquete", 10, y, paint);
        paint.setFakeBoldText(false);
        y += 20;
        canvas.drawText("Tamaño: " + tamano, 10, y, paint);
        y += 20;
        canvas.drawText("Peso: " + peso + " kg", 10, y, paint);
        y += 20;
        canvas.drawText("Longitud: " + longitud + " cm", 10, y, paint);
        y += 20;
        canvas.drawText("Método de pago: " + metodoPago, 10, y, paint);

        // Añadir sección de costos
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("Detalle de Costos", 10, y, paint);
        paint.setFakeBoldText(false);

        // Calcular tarifas
        String tamanoLower = tamano.trim().toLowerCase();
        int tarifaBase = 0;
        if ("pequeño".equals(tamanoLower)) {
            tarifaBase = 15000;
        } else if ("mediano".equals(tamanoLower)) {
            tarifaBase = 30000;
        } else if ("grande".equals(tamanoLower)) {
            tarifaBase = 60000;
        } else {
            tarifaBase = 0;
        }
        boolean express = isExpress;
        int tarifaTotal = tarifaBase + (express ? 25000 : 0);

        y += 20;
        canvas.drawText("Tarifa base (" + tamano + "): $" + String.format("%,d", tarifaBase) + " COP", 10, y, paint);
        if (express) {
            y += 20;
            canvas.drawText("Cargo Express: $25,000 COP", 10, y, paint);
        }
        y += 20;
        paint.setFakeBoldText(true);
        canvas.drawText("Total: $" + String.format("%,d", tarifaTotal) + " COP", 10, y, paint);
        paint.setFakeBoldText(false);

        y += 20;
        canvas.drawText("Tipo de envío: " + (express ? "Express/Premium ⚡" : "Normal"), 10, y, paint);

        y += 40;
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Gracias por usar EncomiendasApp", 150, y, paint);

        pdf.finishPage(page);

        File archivo = null;
        try {
            File carpeta = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            archivo = new File(carpeta, "Factura_" + radicado + ".pdf");
            pdf.writeTo(new FileOutputStream(archivo));
            Toast.makeText(this, "Factura guardada en Descargas ✅", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        pdf.close();
        return archivo;
    }

    private void abrirFacturaPDF() {
        if (archivoPDFGenerado != null && archivoPDFGenerado.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(archivoPDFGenerado), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No hay visor PDF disponible", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se encontró la factura", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_formulario, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_caracteristicas) {
            mostrarCaracteristicasEnvio();
            return true;
        } else if (id == R.id.action_notificacion) {
            crearNotificacionEnvio();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarCaracteristicasEnvio() {
        // Muestra un diálogo con las características del envío
        String mensaje = "Características del envío:\n"
                + "• Envío nacional\n"
                + "• Seguro incluido\n"
                + "• Seguimiento en tiempo real\n"
                + "• Entrega en 24-48 horas";
        new android.app.AlertDialog.Builder(this)
                .setTitle("Características del Envío")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }

    private void crearNotificacionEnvio() {
        String canalId = "envio_notif";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    canalId, "Notificaciones de Envío", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Solicitud de Envío")
                .setContentText("¡Tu solicitud de envío ha sido registrada!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void compartirSolicitud() {
        // Obtén los datos del formulario
        String remitenteNombre = edtRemitenteNombre.getText().toString();
        String destinatarioNombre = edtDestinatarioNombre.getText().toString();
        String tamano = spinnerTamano.getSelectedItem() != null ? spinnerTamano.getSelectedItem().toString() : "";
        String peso = edtPeso.getText().toString();
        String longitud = edtLongitud.getText().toString();

        String texto = "Solicitud de Envío:\n"
                + "Remitente: " + remitenteNombre + "\n"
                + "Destinatario: " + destinatarioNombre + "\n"
                + "Tamaño: " + tamano + "\n"
                + "Peso: " + peso + " kg\n"
                + "Longitud: " + longitud + " cm";

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(sendIntent, "Compartir solicitud con..."));
    }

    // Manejo del resultado de petición de permisos (añadido para SEND_SMS)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // manejar permiso de escritura ya existente si aplicaba
        if (requestCode == REQ_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingSmsNumber != null && pendingSmsBody != null) {
                    sendSmsDirect(pendingSmsNumber, pendingSmsBody);
                    pendingSmsNumber = null;
                    pendingSmsBody = null;
                }
            } else {
                Toast.makeText(this, "Permiso SEND_SMS denegado. No se enviará SMS automáticamente.", Toast.LENGTH_SHORT).show();
                // opcional: abrir app SMS para el usuario
                if (pendingSmsNumber != null && pendingSmsBody != null) {
                    try {
                        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                        smsIntent.setData(Uri.parse("smsto:" + Uri.encode(pendingSmsNumber)));
                        smsIntent.putExtra("sms_body", pendingSmsBody);
                        startActivity(smsIntent);
                    } catch (Exception e) {
                        // ignore
                    }
                    pendingSmsNumber = null;
                    pendingSmsBody = null;
                }
            }
            return;
        }
        // ...existing permission handling (escritura, lectura, etc.)...
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Envío de SMS directo
    private void sendSmsDirect(String number, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);
            Toast.makeText(this, "SMS enviado automáticamente a " + number, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al enviar SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // fallback: abrir app de SMS
            try {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + Uri.encode(number)));
                smsIntent.putExtra("sms_body", message);
                startActivity(smsIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "No hay app de SMS disponible", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mostrarFavoritos() {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c;
        if (usuarioActual == null || usuarioActual.isEmpty()) {
            // fallback: mostrar todas pero avisar
            Toast.makeText(this, "Aviso: usuario no identificado. Mostrando favoritos globales.", Toast.LENGTH_SHORT).show();
            c = db.rawQuery("SELECT nombre, direccion, telefono FROM " + DBHelper.TABLE_FAVORITOS + " ORDER BY nombre", null);
        } else {
            c = db.rawQuery("SELECT nombre, direccion, telefono FROM " + DBHelper.TABLE_FAVORITOS + " WHERE usuario = ? ORDER BY nombre", new String[]{usuarioActual});
        }

        if (c.getCount() == 0) {
            Toast.makeText(this, "No hay direcciones favoritas guardadas", Toast.LENGTH_SHORT).show();
            c.close();
            db.close();
            return;
        }

        ArrayList<String> nombres = new ArrayList<>();
        ArrayList<String> direcciones = new ArrayList<>();
        ArrayList<String> telefonos = new ArrayList<>();
        while (c.moveToNext()) {
            nombres.add(c.getString(0));
            direcciones.add(c.getString(1));
            telefonos.add(c.getString(2));
        }
        c.close();
        db.close();

        CharSequence[] items = nombres.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar dirección favorita")
                .setItems(items, (dialog, which) -> {
                    edtDestinatarioNombre.setText(nombres.get(which));
                    edtDestinatarioDireccion.setText(direcciones.get(which));
                    edtDestinatarioTelefono.setText(telefonos.get(which));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void enviarAlertasUsuario(String usuario, String radicado) {
        // 1. Obtener preferencia y datos de contacto del usuario
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT email, telefono, notif_pref FROM " + DBHelper.TABLE_USUARIOS + " WHERE usuario = ?", new String[]{usuario});
        if (!c.moveToFirst()) {
            c.close();
            db.close();
            return;
        }
        String email = c.getString(0);
        String telefono = c.getString(1);
        String pref = c.getString(2);
        c.close();
        db.close();

        String mensaje = "¡Tu pedido ha sido registrado!\nRadicado: " + radicado + "\nGracias por usar EncomiendasApp.";

        // 2. Enviar según preferencia
        if ("sms".equalsIgnoreCase(pref)) {
            enviarSMSAlerta(telefono, mensaje);
        } else if ("both".equalsIgnoreCase(pref)) {
            enviarSMSAlerta(telefono, mensaje);
            enviarEmailAlerta(email, radicado, mensaje);
        } else {
            enviarEmailAlerta(email, radicado, mensaje);
        }
    }

    // Enviar SMS (pide permiso si es necesario)
    private void enviarSMSAlerta(String telefono, String mensaje) {
        if (telefono == null || telefono.trim().isEmpty()) {
            Toast.makeText(this, "No hay teléfono registrado para enviar SMS", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingSmsNumber = telefono;
            pendingSmsBody = mensaje;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS);
            return;
        }
        sendSmsDirect(telefono, mensaje);
    }

    // Enviar Email usando Intent
    private void enviarEmailAlerta(String email, String radicado, String mensaje) {
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "No hay email registrado para enviar alerta", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Confirmación de pedido - Radicado " + radicado);
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);
        try {
            startActivity(Intent.createChooser(intent, "Enviar confirmación con..."));
        } catch (Exception e) {
            Toast.makeText(this, "No hay app de correo disponible", Toast.LENGTH_SHORT).show();
        }
    }
}


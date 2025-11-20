package co.edu.appencomiendas;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "encomiendas.db";
	public static final int DATABASE_VERSION = 12; // bumped to apply tracking table migration

	public static final String TABLE_PAQUETES = "paquetes";
	public static final String TABLE_USUARIOS = "usuarios";
	public static final String TABLE_FAVORITOS = "favoritos";
	public static final String TABLE_OPINIONES = "opiniones";
	public static final String TABLE_TRACKING_POINTS = "tracking_points";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PAQUETES + " (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"radicado TEXT UNIQUE, " +
				"remitenteNombre TEXT, " +
				"remitenteTelefono TEXT, " +
				"remitenteDireccion TEXT, " +
				"destinatarioNombre TEXT, " +
				"destinatarioTelefono TEXT, " +
				"destinatarioDireccion TEXT, " +
				"tamano TEXT, " +
				"peso TEXT, " +
				"longitud TEXT, " +
				"metodoPago TEXT, " +
				"estado TEXT DEFAULT 'Pendiente', " + // Solo 'Pendiente' y 'Entregado'
				"calificacion INTEGER DEFAULT 0, " +
				"latitud REAL, " +
				"longitudGeo REAL, " +
				"express INTEGER DEFAULT 0, " +
				"tarifa TEXT" +
				")");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USUARIOS + " (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"nombreCompleto TEXT, " +
				"usuario TEXT UNIQUE, " +
				"email TEXT, " +
				"direccion TEXT, " +
				"password TEXT, " +
				"fechaNacimiento TEXT, " +
				"genero TEXT, " +
				"rol TEXT, " +
				"telefono TEXT, " +
				"notif_pref TEXT DEFAULT 'email' " +
				")");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITOS + " (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"nombre TEXT, " +
				"direccion TEXT, " +
				"telefono TEXT, " +
				"usuario TEXT, " +
				"latitud REAL, " +
				"longitud REAL, " +
				"tipo TEXT" +
				")");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_OPINIONES + " (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"radicado TEXT UNIQUE, " +
				"usuario TEXT, " +
				"calificacion INTEGER, " +
				"comentario TEXT, " +
				"fecha TEXT, " +
				"FOREIGN KEY(radicado) REFERENCES " + TABLE_PAQUETES + "(radicado)" +
				")");

		// nueva tabla para puntos de rastreo
		db.execSQL("CREATE TABLE IF NOT EXISTS tracking_points (" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"radicado TEXT, " +
				"latitud REAL, " +
				"longitud REAL, " +
				"timestamp TEXT" +
				")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 9) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_OPINIONES + " (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"radicado TEXT UNIQUE, " +
					"usuario TEXT, " +
					"calificacion INTEGER, " +
					"comentario TEXT, " +
					"fecha TEXT, " +
					"FOREIGN KEY(radicado) REFERENCES " + TABLE_PAQUETES + "(radicado)" +
					")");
		}
		if (oldVersion < 10) {
			// Asegurar que la tabla existe si no se creó en versiones anteriores
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_OPINIONES + " (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"radicado TEXT UNIQUE, " +
					"usuario TEXT, " +
					"calificacion INTEGER, " +
					"comentario TEXT, " +
					"fecha TEXT, " +
					"FOREIGN KEY(radicado) REFERENCES " + TABLE_PAQUETES + "(radicado)" +
					")");
		}
		if (oldVersion < 11) {
			// Añadir columna telefono si no existe
			try {
				db.execSQL("ALTER TABLE " + TABLE_FAVORITOS + " ADD COLUMN telefono TEXT");
			} catch (Exception ignored) {}
			try {
				db.execSQL("ALTER TABLE " + TABLE_FAVORITOS + " ADD COLUMN usuario TEXT");
			} catch (Exception ignored) {}
		}
		if (oldVersion < 12) {
			try {
				db.execSQL("CREATE TABLE IF NOT EXISTS tracking_points (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"radicado TEXT, " +
						"latitud REAL, " +
						"longitud REAL, " +
						"timestamp TEXT" +
						")");
			} catch (Exception ignored) {}
		}
		// Si necesitas migrar estados antiguos:
		if (oldVersion < 13) {
			try {
				db.execSQL("UPDATE " + TABLE_PAQUETES + " SET estado = 'Pendiente' WHERE estado NOT IN ('Entregado')");
			} catch (Exception ignored) {}
		}
	}

	// Nuevo: asegura que la tabla favoritos exista (útil cuando la BD fue creada con versión antigua)
	public void ensureFavoritosTable(SQLiteDatabase db) {
		if (db != null && db.isOpen()) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITOS + " (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"nombre TEXT, " +
					"direccion TEXT, " +
					"telefono TEXT, " +
					"usuario TEXT, " +
					"latitud REAL, " +
					"longitud REAL, " +
					"tipo TEXT" +
					")");
		}
	}
}

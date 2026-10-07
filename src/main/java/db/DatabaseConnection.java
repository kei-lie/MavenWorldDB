package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
	private static final String URL = 
			"jdbc:mysql://localhost:3306/world"
			+ "?useSSL=false"
			+ "&allowPublicKeyRetrieval=true"
			+ "&serverTimeZone=Europe/Riga"
			+ "&characterEncoding=utf8"
			+ "&useUnicode=true";
	
	private static final String USER = "root";
	private static final String PASSWORD = "KinichGlazer67";
	
	public static Connection getConnection() throws SQLException{
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}
}

package de.unifrankfurt.informatik.acoli.fid.spider;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Statistics {
	
		private static long processedURLs = 0;
		private static Date startTime;
		private static Date endTime;
		
		
		public static synchronized void initialize() {
			processedURLs = 0;
			startTime = new Date();
			endTime = new Date();
		}
	 
		public static synchronized void incrURLCounter() {
			processedURLs++;
		}
		
		public static synchronized long getURLCounter() {
			return processedURLs;
		}
		
		public static void setStartDate(Date date) {
			startTime = date;
		}
		
		public static synchronized Date getStartDate() {
			return startTime;
		}
		
		public static synchronized void setEndDate(Date date) {
			endTime = date;
		}
		
		public static synchronized Date getEndDate() {
			return endTime;
		}
		
		public static synchronized void printRuntime() {
			Date diff = new Date(endTime.getTime() - startTime.getTime());

			Calendar calendar = Calendar.getInstance();
			calendar.setTimeZone(TimeZone.getTimeZone("Berlin"));
			calendar.setTime(diff);
			
			System.out.println("\nTotal time :");
			System.out.println(calendar.get(Calendar.HOUR_OF_DAY)+" h");
			System.out.println(calendar.get(Calendar.MINUTE)+" min");
			System.out.println(calendar.get(Calendar.SECOND)+" sec");
			}
		
		
		public static synchronized void printReport() {
			
			System.out.println("\n------------------------");
			System.out.println("Processed URLs : "+processedURLs);
			printRuntime();
			
			
		}
		
		public static void main (String [] args) {
			Statistics.initialize();
			Statistics.printReport();
		}
		
}

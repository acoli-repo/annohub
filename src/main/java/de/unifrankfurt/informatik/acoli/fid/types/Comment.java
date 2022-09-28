package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.util.Date;

/**
 * @author frank
 *
 */
public class Comment implements Comparable<Comment>, Serializable {

		/**
	 * 
	 */
	private static final long serialVersionUID = -7262910083480103978L;
		private int id=-1;
		private long date=0L;
		private String userId;
		private String title;
		private String text;
		private int relatesToPostId;
		
		public Comment() {}
		
		public Comment(int id, String userID, String title, String text, int asAnswerToPostId) {
			this.setId(id);
			this.setUserId(userID);
			this.setTitle(title);
			this.setText(text);
			this.setDate(new Date().getTime());
			this.setRelatedPostId(asAnswerToPostId);
		}


		public int getId() {
			return id;
		}


		public void setId(int id) {
			this.id = id;
		}


		public long getDate() {
			return date;
		}


		public void setDate(long date) {
			this.date = date;
		}


		public String getUserId() {
			return userId;
		}


		public void setUserId(String userID) {
			this.userId = userID;
		}


		public String getTitle() {
			return title;
		}


		public void setTitle(String title) {
			this.title = title;
		}


		public String getText() {
			return text;
		}


		public void setText(String text) {
			this.text = text;
		}


		public int getRelatedPostId() {
			return relatesToPostId;
		}


		public void setRelatedPostId(int relatedPostId) {
			this.relatesToPostId = relatedPostId;
		}
		

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Comment other) {
			return Integer.compare(getId(), other.getId());
		}
		
		public String asRawText() {
			return (title+" "+text+" "+relatesToPostId).replaceAll("\n|\r", "");
		}

}

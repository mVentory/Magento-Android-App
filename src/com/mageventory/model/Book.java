package com.mageventory.model;

import java.io.Serializable;

public class Book implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String title;
	private String description;
	private String authors;
	private String publishDate;
	private String iSBN_10;
	private String iSBN_13;
	private String thumbnail_link;
	private String previewLink;
	private String infoLink;
	
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the authors
	 */
	public String getAuthors() {
		return authors;
	}
	/**
	 * @param authors the authors to set
	 */
	public void setAuthors(String authors) {
		this.authors = authors;
	}
	/**
	 * @return the publishDate
	 */
	public String getPublishDate() {
		return publishDate;
	}
	/**
	 * @param publishDate the publishDate to set
	 */
	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}
	/**
	 * @return the iSBN_10
	 */
	public String getiSBN_10() {
		return iSBN_10;
	}
	/**
	 * @param iSBN_10 the iSBN_10 to set
	 */
	public void setiSBN_10(String iSBN_10) {
		this.iSBN_10 = iSBN_10;
	}
	/**
	 * @return the iSBN_13
	 */
	public String getiSBN_13() {
		return iSBN_13;
	}
	/**
	 * @param iSBN_13 the iSBN_13 to set
	 */
	public void setiSBN_13(String iSBN_13) {
		this.iSBN_13 = iSBN_13;
	}
	/**
	 * @return the thumbnail_link
	 */
	public String getThumbnail_link() {
		return thumbnail_link;
	}
	/**
	 * @param thumbnail_link the thumbnail_link to set
	 */
	public void setThumbnail_link(String thumbnail_link) {
		this.thumbnail_link = thumbnail_link;
	}
	/**
	 * @return the previewLink
	 */
	public String getPreviewLink() {
		return previewLink;
	}
	/**
	 * @param previewLink the previewLink to set
	 */
	public void setPreviewLink(String previewLink) {
		this.previewLink = previewLink;
	}
	/**
	 * @return the infoLink
	 */
	public String getInfoLink() {
		return infoLink;
	}
	/**
	 * @param infoLink the infoLink to set
	 */
	public void setInfoLink(String infoLink) {
		this.infoLink = infoLink;
	}
}

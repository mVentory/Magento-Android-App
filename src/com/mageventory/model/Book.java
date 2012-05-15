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
	private String thumbnail;
	private String previewLink;
	private String infoLink;
	private String id;
	private String selfLink;
	private String publisher;
	private String pageCount;
	private String averageRate;
	private String rateCount;
	private String smallThumbnail;	
	private String viewability;
	private String embeddable;
	private String webReadedLink;
	private String textSnippet;
	private String language;
	
	
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
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the selfLink
	 */
	public String getSelfLink() {
		return selfLink;
	}
	/**
	 * @param selfLink the selfLink to set
	 */
	public void setSelfLink(String selfLink) {
		this.selfLink = selfLink;
	}
	/**
	 * @return the publisher
	 */
	public String getPublisher() {
		return publisher;
	}
	/**
	 * @param publisher the publisher to set
	 */
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	/**
	 * @return the pageCount
	 */
	public String getPageCount() {
		return pageCount;
	}
	/**
	 * @param pageCount the pageCount to set
	 */
	public void setPageCount(String pageCount) {
		this.pageCount = pageCount;
	}
	/**
	 * @return the averageRate
	 */
	public String getAverageRate() {
		return averageRate;
	}
	/**
	 * @param averageRate the averageRate to set
	 */
	public void setAverageRate(String averageRate) {
		this.averageRate = averageRate;
	}
	/**
	 * @return the rateCount
	 */
	public String getRateCount() {
		return rateCount;
	}
	/**
	 * @param rateCount the rateCount to set
	 */
	public void setRateCount(String rateCount) {
		this.rateCount = rateCount;
	}
	/**
	 * @return the smallThumbnail
	 */
	public String getSmallThumbnail() {
		return smallThumbnail;
	}
	/**
	 * @param smallThumbnail the smallThumbnail to set
	 */
	public void setSmallThumbnail(String smallThumbnail) {
		this.smallThumbnail = smallThumbnail;
	}
	/**
	 * @return the thumbnail
	 */
	public String getThumbnail() {
		return thumbnail;
	}
	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	/**
	 * @return the viewability
	 */
	public String getViewability() {
		return viewability;
	}
	/**
	 * @param viewability the viewability to set
	 */
	public void setViewability(String viewability) {
		this.viewability = viewability;
	}
	/**
	 * @return the embeddable
	 */
	public String getEmbeddable() {
		return embeddable;
	}
	/**
	 * @param embeddable the embeddable to set
	 */
	public void setEmbeddable(String embeddable) {
		this.embeddable = embeddable;
	}
	/**
	 * @return the webReadedLink
	 */
	public String getWebReadedLink() {
		return webReadedLink;
	}
	/**
	 * @param webReadedLink the webReadedLink to set
	 */
	public void setWebReadedLink(String webReadedLink) {
		this.webReadedLink = webReadedLink;
	}
	/**
	 * @return the textSnippet
	 */
	public String getTextSnippet() {
		return textSnippet;
	}
	/**
	 * @param textSnippet the textSnippet to set
	 */
	public void setTextSnippet(String textSnippet) {
		this.textSnippet = textSnippet;
	}
	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}
	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	
}

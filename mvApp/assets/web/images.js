/**
 * Javascript for the images.html
 */

/**
 * Images URLs passed via addImageUrl method
 */
var sImageUrls = [];
/**
 * img DOM elements added to the body ordered by image size
 */
var sAddedImages = [];
/**
 * Minimum dimension for the image. All images with the at least one dimension
 * less than 100 pixels will be ignored
 */
var MINIMUM_IMAGE_DIMENSION = 100;
/**
 * Message which is shown when no images of the required size are found
 */
var sNoImagesMessage;
/**
 * The images loader count
 */
var sLoaders = 0;

/**
 * Add format feature to the String type
 * 
 * http://stackoverflow.com/a/4673436/527759
 */
if (!String.prototype.format) {
	String.prototype.format = function() {
		var args = arguments;
		return this.replace(/{(\d+)}/g, function(match, number) {
			return typeof args[number] != 'undefined' ? args[number] : match;
		});
	};
}
/**
 * ImageContainer object to help build custom views for the images
 * 
 * @param url the image URL
 * @param img the img element
 */
function ImageContainer(url, img) {
	this.url = url;
	this.img = img;
}
/**
 * Construct the more complex dom element for the img element with the save button,
 * image information and so forth
 * 
 * @param saveButtonText the label for the save button
 */
ImageContainer.prototype.getDomElement = function(saveButtonText) {
	if (this.domElement == null) {
		// create parent container
		var div = document.createElement("div");
		div.setAttribute("align", "center");
		// create table with the image size information and button
		var table = document.createElement("table");
		var row = document.createElement("tr");
		var column = document.createElement("td");
		column.style.width = "100%";
		this.imageStatusText = column;
		row.appendChild(column);
		column = document.createElement("td");
		var button = document.createElement("button");
		var parent = this;
		// initialize button on click listener
		button.onclick = function() {
			window.HTMLOUT.saveImage(parent.url);
		};
		button.innerHTML = saveButtonText;
		column.appendChild(button);
		row.appendChild(column);
		table.appendChild(row);
		// append img element to the parent container
		div.appendChild(this.img);
		// append information table to the parent container
		div.appendChild(table);
		// add the line separator
		var hr = document.createElement("hr");
		div.appendChild(hr);
		this.domElement = div;
	}
	return this.domElement;
}

/**
 * Update the image status text after the image is loaded
 * 
 * @param imageSizeText the formatting string to display image size
 */
ImageContainer.prototype.updateImageStatus = function(imageSizeText) {
	if (this.imageStatusText != null) {
		// if imageStatusText is initialized
		this.imageStatusText.innerHTML = imageSizeText.format(this.img.width,
				this.img.height);
	}
}
/**
 * Load images stored in the sImageUrls array
 * 
 * @param imageSizeText
 *            the formatting string to show image size
 * @param saveButtonText
 *            the text for the save button
 * @param noImagesMessage
 *            the message which should be shown when no valid images are
 *            downloaded
 */
function loadImages(imageSizeText, saveButtonText, noImagesMessage) {
	// save noImagesMessage to global variable
    sNoImagesMessage = noImagesMessage;
	// iterate through image urls and init corresponding DOM elements
	for (var i = 0; i < sImageUrls.length; i++) {
		// create img DOM element
		var img = document.createElement("img");
		// initialize onload action
		img.onload = function() {
			window.HTMLOUT.debug("img.onload");
			if (this.width < MINIMUM_IMAGE_DIMENSION
					|| this.height < MINIMUM_IMAGE_DIMENSION) {
				// if at least one image dimension is smalle than minimum
				// allowed
				window.HTMLOUT.debug("img.onload: too small image "
						+ this.width + "x" + this.height + ", skipping");
				// notify android side that image is downloaded
				stopLoading();
				// interrupt method invocation
				return;
			}

			// set the img element document id
			var id = this.myId;
			this.id = id;
			// reference to the node current img element should be inserted
			// before if found
			var insertBeforeNode = null;
			// downloaded image square size
			var size = this.width * this.height;
			window.HTMLOUT.debug("img.onload: size: " + this.width + "x"
					+ this.height);
			// iterate through already downloaded and added images to determine
			// the position where the current image should be added
			for (var j = 0; j < sAddedImages.length; j++) {
				var addedImage = sAddedImages[j];
				if (size > addedImage.width * addedImage.height) {
					// if current image square size is more than the added
					// before image size

					// remember the reference to the added before image. Current
					// image should be inserted befor it
					insertBeforeNode = addedImage;
					// insert current image to the sAddedImages array before the
					// position of found added before image
					sAddedImages.splice(j, 0, this);
					break;
				}
			}
			if (insertBeforeNode == null) {
				// if there are no nodes the image should be inserted before
				document.body.appendChild(this.imageContainer.getDomElement(saveButtonText));
				// add the current image to the end of sAddedImages array
				sAddedImages.push(this);
			} else {
				window.HTMLOUT.debug("img.onload: insert before: this "
						+ this.width + "x" + this.height + " before "
						+ insertBeforeNode.width + "x"
						+ insertBeforeNode.height);
				// insert current img element before found node
				document.body.insertBefore(this.imageContainer.getDomElement(saveButtonText), insertBeforeNode.imageContainer.getDomElement(saveButtonText));
			}
			// show the image size information
            this.imageContainer.updateImageStatus(imageSizeText);
			// notify android side that image is downloaded
			stopLoading();
		}
		// initialize onerror action
		img.onerror = function() {
			window.HTMLOUT.debug("img.error");
			// notify android side that image download failed
			window.HTMLOUT.stopImageLoading();
		}
		img.myId = i;
        var url = sImageUrls[i];
		img.src = url;
		// construct image container
    	img.imageContainer = new ImageContainer(url, img);
		// make the image fit the screen (currently disabled)
		// img.style.maxWidth = "100%";
	}
}

/**
 * The method which is called when image starts loading. The call is passed to
 * the Android side
 */
function startLoading() {
	// pass the call to Android side
	window.HTMLOUT.startImageLoading();
	// increment loader counter
	sLoaders++;
}

/**
 * The method which is called when image stops loading (either success or
 * error). The call is passed to the Android side
 */
function stopLoading() {
	// pass the call to Android side
	window.HTMLOUT.stopImageLoading();
	// decrement loader counter
	sLoaders--;
	if (sLoaders == 0 && sAddedImages.length == 0) {
		// if all images are loaded but there are no added valid images show the
		// no images message
		showCenteredMessage(sNoImagesMessage);
	}
}
/**
 * Add the image to the sImageUrls array
 * 
 * @param imageUrl
 *            the url of the image to add
 */
function addImageUrl(imageUrl) {
	window.HTMLOUT.debug("addImageUrl: " + imageUrl);
	sImageUrls.push(imageUrl);
	startLoading();
}

/**
 * Show the message at the center of the page
 * 
 * @param message
 */
function showCenteredMessage(message){
    document.body.style.width = "100%";
    document.body.style.height = "100%";
    var div = document.createElement("div");
    div.style.top="50%";
    div.style.left="50%";
    div.style.position="absolute";
    div.innerHTML=message;
    document.body.appendChild(div);
}
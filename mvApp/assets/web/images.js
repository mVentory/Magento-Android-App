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
 * less than 300 pixels will be ignored
 */
var MINIMUM_IMAGE_DIMENSION = 300;
/**
 * Load images stored in the sImageUrls array
 */
function loadImages() {
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
				window.HTMLOUT.stopImageLoading();
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
				document.body.appendChild(this);
				// add the current image to the end of sAddedImages array
				sAddedImages.push(this);
			} else {
				window.HTMLOUT.debug("img.onload: insert before: this "
						+ this.width + "x" + this.height + " before "
						+ insertBeforeNode.width + "x"
						+ insertBeforeNode.height);
				// insert current img element before found node
				document.body.insertBefore(this, insertBeforeNode);
			}
			// notify android side that image is downloaded
			window.HTMLOUT.stopImageLoading();
		}
		// initialize onerror action
		img.onerror = function() {
			window.HTMLOUT.debug("img.error");
			// notify android side that image download failed
			window.HTMLOUT.stopImageLoading();
		}
		img.myId = i;
		img.src = sImageUrls[i];
		// make the image fit the screen
		img.style.maxWidth = "100%";
	}
}
/**
 * Add the image to the sImageUrls array
 * 
 * @param imageUrl the url of the image to add
 */
function addImageUrl(imageUrl) {
	window.HTMLOUT.debug("addImageUrl: " + imageUrl);
	sImageUrls.push(imageUrl);
	window.HTMLOUT.startImageLoading();
}
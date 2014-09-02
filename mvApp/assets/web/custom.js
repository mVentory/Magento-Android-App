/**
 * Set collection implementation
 */
function Set() {
	this.content = {};
}

/**
 * Method to add element to set
 */
Set.prototype.add = function(val) {
	this.content[val] = true;
}

/**
 * Method to remove element from set
 */
Set.prototype.remove = function(val) {
	delete this.content[val];
}

/**
 * Method to check whether the element is in set
 */
Set.prototype.contains = function(val) {
	return (val in this.content);
}

/**
 * Get the set content as array
 */
Set.prototype.asArray = function() {
	var res = [];
	for ( var val in this.content)
		res.push(val);
	return res;
}

/**
 * Key tags used in expandSelectionToElement method to determine key selection
 * expanding nodes
 */
var KEY_TAGS = new Set();

/*
 * Init KEY_TAGS
 */
{
	KEY_TAGS.add("P");
	KEY_TAGS.add("A");
	KEY_TAGS.add("DIV");
	KEY_TAGS.add("SPAN");
	KEY_TAGS.add("LI");
	KEY_TAGS.add("UL");
	KEY_TAGS.add("TD");
	KEY_TAGS.add("TR");
	KEY_TAGS.add("TABLE");
	KEY_TAGS.add("BODY");
}

/**
 * Expand the selection from the word to the containing node or node parent
 */
function expandSelectionToElement() {
	var range;
	if (window.getSelection && document.createRange) {
		range = document.createRange();
		var sel = window.getSelection();
		var el = sel.anchorNode;
		var i = 0;
		// determine parent node with the deepness specified by sSelectionLevel
		// variableexpandSelectionToElement
		while (el != null && i < sSelectionLevel) {
			var parentNode = el.parentNode;
			if (parentNode == null)
				break;
			el = parentNode;
			window.HTMLOUT.debug('i: ' + i + ' tag ' + el.tagName);
			// check whether the element is a key tag
			if (KEY_TAGS.contains(el.tagName)) {
				i++;
			}
		}
		sSelectionLevel++;
		range.selectNodeContents(el);
		sel.addRange(range);
	}
}

/**
 * Get the selected text as string. This is used to pass the selection to the
 * java side
 */
function getSelectionText() {
	var text = "";
	if (window.getSelection) {
		text = window.getSelection().toString();
	} else if (document.selection && document.selection.type != "Control") {
		text = document.selection.createRange().text;
	}
	return text;
}

/**
 * Interval time for the expandSelectionToElement calls
 */
var LONG_TOUCH_INTERVAL_TIME = 1250;

/**
 * Current selection level. May be adjusted in various places
 */
var sSelectionLevel;

/**
 * Reference to the scheduled interval so it may be cancelled in the
 * globalOnMouseUp method
 */
var sInterval;

/**
 * The method which is called from the Java code when the mouse down event
 * occurs in the WebView
 */
function globalOnMouseDown() {
	window.HTMLOUT.debug('globalOnMouseDown');
	// reset selection level
	sSelectionLevel = 1;
	clearIntervalIfNecessary();
	// schedule the selection expanding each 1250 milliseconds
	sInterval = setInterval(function() {
		window.HTMLOUT.debug('interval');
		if(!window.HTMLOUT.isInActionMode()){
			window.HTMLOUT.debug('interval: not in action mode, canceling');
			clearIntervalIfNecessary();
			return;
		}
		if(!window.HTMLOUT.isTouching()){
			window.HTMLOUT.debug('interval: pointer is not down anymore, canceling');
			clearIntervalIfNecessary();
			return;
		}
		expandSelectionToElement();
		window.HTMLOUT.performHapticFeedback();
	}, LONG_TOUCH_INTERVAL_TIME);
}

/**
 * Clear the sInterval if it is still scheduled
 */
function clearIntervalIfNecessary(){
	if(sInterval != null){
		clearInterval(sInterval);
		sInterval = null;
	}
		
}
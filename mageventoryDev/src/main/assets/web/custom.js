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
	KEY_TAGS.add("UL");
	KEY_TAGS.add("TD");
	KEY_TAGS.add("TR");
	KEY_TAGS.add("TABLE");
	KEY_TAGS.add("BODY");
}
/**
 * Tags used in getHtmlOfSelection method to determine whether the tag should be
 * kept in addition to block level elements
 */
var TAGS_TO_KEEP = new Set();

/*
 * Init TAGS_TO_KEEP
 */
{
	TAGS_TO_KEEP.add("LI");
	TAGS_TO_KEEP.add("SPAN");
	TAGS_TO_KEEP.add("TD");
	TAGS_TO_KEEP.add("TR");
}

/**
 * List of block level elements tag names
 */
var BLOCK_LEVEL_ELEMENTS = new Set();

/*
 * Init BLOCK_LEVEL_ELEMENTS
 */
{
    BLOCK_LEVEL_ELEMENTS.add("ADDRESS");
    BLOCK_LEVEL_ELEMENTS.add("ARTICLE");
    BLOCK_LEVEL_ELEMENTS.add("ASIDE");
    BLOCK_LEVEL_ELEMENTS.add("AUDIO");
    BLOCK_LEVEL_ELEMENTS.add("BLOCKQUOTE");
    BLOCK_LEVEL_ELEMENTS.add("CANVAS");
    BLOCK_LEVEL_ELEMENTS.add("DD");
    BLOCK_LEVEL_ELEMENTS.add("DIV");
    BLOCK_LEVEL_ELEMENTS.add("DL");
    BLOCK_LEVEL_ELEMENTS.add("FIELDSET");
    BLOCK_LEVEL_ELEMENTS.add("FIGCAPTION");
    BLOCK_LEVEL_ELEMENTS.add("FIGURE");
    BLOCK_LEVEL_ELEMENTS.add("FOOTER");
    BLOCK_LEVEL_ELEMENTS.add("FORM");
    BLOCK_LEVEL_ELEMENTS.add("H1");
    BLOCK_LEVEL_ELEMENTS.add("H2");
    BLOCK_LEVEL_ELEMENTS.add("H3");
    BLOCK_LEVEL_ELEMENTS.add("H4");
    BLOCK_LEVEL_ELEMENTS.add("H5");
    BLOCK_LEVEL_ELEMENTS.add("H6");
    BLOCK_LEVEL_ELEMENTS.add("HEADER");
    BLOCK_LEVEL_ELEMENTS.add("HGROUP");
    BLOCK_LEVEL_ELEMENTS.add("HR");
    BLOCK_LEVEL_ELEMENTS.add("NOSCRIPT");
    BLOCK_LEVEL_ELEMENTS.add("OL");
    BLOCK_LEVEL_ELEMENTS.add("OUTPUT");
    BLOCK_LEVEL_ELEMENTS.add("P");
    BLOCK_LEVEL_ELEMENTS.add("PRE");
    BLOCK_LEVEL_ELEMENTS.add("SECTION");
    BLOCK_LEVEL_ELEMENTS.add("TABLE");
    BLOCK_LEVEL_ELEMENTS.add("TFOOT");
    BLOCK_LEVEL_ELEMENTS.add("UL");
    BLOCK_LEVEL_ELEMENTS.add("VIDEO");
}

/**
 * List of tags which should be skipped in the filterHtml method
 */
var TAGS_TO_SKIP = new Set(); 

/*
 * Init TAGS_TO_SKIP
 */
{
    TAGS_TO_SKIP.add("STYLE");
    TAGS_TO_SKIP.add("SCRIPT");
    TAGS_TO_SKIP.add("VIDEO");
    TAGS_TO_SKIP.add("CANVAS");
    TAGS_TO_SKIP.add("NOSCRIPT");
}

/**
 * Node type of the text nodes
 */
var TEXT_NODE = 3;

/**
 * Line separator
 */
var LINE_SEPARATOR = "\n";
/**
 * HTML line separator
 */
var HTML_LINE_SEPARATOR = "<br>\n";

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
 * Remove all leading line separators from parsed text and join lines into
 * single string.
 * 
 * @param lines
 *            the text lines to filter and join
 * @returns filtered joined string
 */
function filterAndJoinParsedText(lines){
	var i = lines.length - 1;
	// remove all ending line separators
	while (i >= 0 && lines[i] === LINE_SEPARATOR) {
		lines.splice(i, 1);
		i--;
	}
	return lines.join("");
}

/**
 * Get the whole web page text as string. This is used to pass the text to the
 * java side
 */
function getPageSimplifiedHtml(){
	var res = [];
	// get filtered string from the HTML
	filterHtml(document.body, res, true);
	// join lines and filter final text
	return filterAndJoinParsedText(res);
}

/**
 * Get the selected text as string. This is used to pass the selection to the
 * java side
 */
function getSelectionText() {
	var str = getHtmlOfSelection();
	// filtered strings array
	var res = [];
	// create DOM element with the content received from getHtmlOfSelection
	var div = document.createElement('div');
	div.innerHTML = str;
	// get filtered string from the HTML
	filterHtml(div, res, false);
	return filterAndJoinParsedText(res);
}
/**
 * Get the raw HTML string of the selected area
 * 
 * http://stackoverflow.com/a/5084044/527759
 * 
 * @returns raw HTML string
 */
function getHtmlOfSelection() {
	var range;
	if (document.selection && document.selection.createRange) {
		range = document.selection.createRange();
		return range.htmlText;
	} else if (window.getSelection) {
		var selection = window.getSelection();
		if (selection.rangeCount > 0) {
			range = selection.getRangeAt(0);
			var clonedSelection = range.cloneContents();
			var div = document.createElement('div');
			div.appendChild(clonedSelection);
			return div.innerHTML;
		} else {
			return '';
		}
	} else {
		return '';
	}
}
/**
 * Get the string representation of the DOM element
 * 
 * @param el
 *            the element to get the filtered text from
 * @param res
 *            the global strings result holder
 * @param keepBlockLevelTags
 *            whether the block level tags should be kept without any attributes
 * @returns the updated res
 */
function filterHtml(el, res, keepBlockLevelTags) {
	var elements = el.childNodes;
	if (elements != null) {
		// iterate through elements and process recursively
		for (var i = 0; i < elements.length; i++) {
			var child = elements[i];
			if (TAGS_TO_SKIP.contains(child.tagName)) {
				// the processing child has a tag name which is marked to be
				// skipped
				continue;
			}
			// is it block level element
			var isBlockLevelElement = BLOCK_LEVEL_ELEMENTS
					.contains(child.tagName);
			if (isBlockLevelElement && res.length != 0 && !keepBlockLevelTags) {
				// if is block level element and res is already not empty and
				// keepBlockLevelTags is false
				addLineSeparator(res);
				addLineSeparator(res);
			}
			if (keepBlockLevelTags
					&& (isBlockLevelElement || TAGS_TO_KEEP
							.contains(child.tagName))) {
				// if keepBlockLevelTags is true and current element is block
				// level or tag which should be kept
				res.push("<" + child.tagName + ">");
			}
			if (child.tagName === "BR") {
				// if this is a line break tag
				if(!keepBlockLevelTags){
					addLineSeparator(res, true);
				} else {
					res.push(HTML_LINE_SEPARATOR);
				}
			}
			if (child.tagName === "LI" && !keepBlockLevelTags) {
				// if this is a list item tag and keepBlockLevelTags is false
				if (res.length != 0) {
					// add empty line only in case some text is already present
					// in the result
					addLineSeparator(res);
				}
				res.push("• ");
			}

			if (child.nodeType == TEXT_NODE) {
				// if this is a text node
				if (child.nodeValue !== null) {
					// if node value is not null
					var nodeValue = child.nodeValue
					.replace(/<!--[\s\S]*?-->/g,"") // remove all HTML comments
					.replace(/\s{2,}/g, " ") // remove all duplicated spaces
					.replace(/^\n+/g, "") // remove all leading line breaks
					.replace(/\n+$/g, "") // remove all trailing line breaks
					if (nodeValue.length > 0 && nodeValue !== " ") {
						// if filtered node value is not empty or not equals to
						// space
						res.push(nodeValue);
					}
				}
			} else {
				// convert current element childs to string
				filterHtml(child, res, keepBlockLevelTags);
			}
			if (keepBlockLevelTags
					&& (isBlockLevelElement || TAGS_TO_KEEP
							.contains(child.tagName))) {
				// if keepBlockLevelTags is true and current element is block
				// level or tag which should be kept
				res.push("</" + child.tagName + ">");
			}
			if (isBlockLevelElement && !keepBlockLevelTags) {
				// add empty line for blocking element
				addLineSeparator(res);
				addLineSeparator(res);
			}
		}
	}
	return res;
}

/**
 * Add line separator to the array if there were no 2 sequential line separators
 * at the end
 * 
 * @param text
 *            the array of strings
 * @param force
 *            whether to add line separator to the end even if there are 2
 *            sequential line separators in the end of array
 */		
function addLineSeparator(text, force) {
	if (force
			|| text.length <= 1
			|| !(text[text.length - 1] === text[text.length - 2] && text[text.length - 1] === LINE_SEPARATOR)) {
		// if add line separator forced or there are no 2 sequential line
		// separators in the end of array
		text.push(LINE_SEPARATOR);
	}
}

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
ZoomImageView
=============

ZoomImageView is an Android ImageView with zoom functionality, all in one class.

Works great with ViewPager, example provided.

Used in [Couple](https://play.google.com/store/apps/details?id=com.tenthbit.juliet "Couple")  app.


Features
-

* Multi-touch pinch zoom
* Double-tap zoom
* Smooth zooming and scrolling, using animation thread
* All in one class
* Easy to put in a scrolling parent, like the ViewPager (example provided)



Examples
-
This project provides two examples:

* Simple use of ImageViewZoom in [OneImageSampleActivity](https://github.com/tenthbitinc/ZoomImageView/blob/master/src/com/tenthbit/zoomimageview/sample/OneImageSampleActivity.java "OneImageSampleActivity")
* Use of ImageViewZoom with ViewPager in [ViewPagerSampleActivity](https://github.com/tenthbitinc/ZoomImageView/blob/master/src/com/tenthbit/zoomimageview/sample/ViewPagerSampleActivity.java "ViewPagerSampleActivity") 

Support
-
Android 2.2 (API 8) and newer.

Origin
-
This project is based on the excellent [PhotoView](https://github.com/chrisbanes/PhotoView "PhotoView") by Chris Banes.
Many thanks to Chris for writing it.

Improvements over PhotoView:
- The library has been simplified from seven files to just one.
- Fixed pinch zoom accuracy, also with multiple fingers.
- Some parts of code have been removed, rewritten and overall simplified.

License
-
[Apache Version 2](http://www.apache.org/licenses/LICENSE-2.0.html "Apache Version 2")

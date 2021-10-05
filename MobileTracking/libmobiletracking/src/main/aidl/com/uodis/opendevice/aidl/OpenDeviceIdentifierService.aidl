// OpenDeviceIdentifierService.aidl
package com.uodis.opendevice.aidl;

// Declare any non-default types here with import statements

interface OpenDeviceIdentifierService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    /** 获取OAID */

       String getOaid();

       /** 获取限制跟踪参数，true：限制跟踪；false：不限制跟踪*/

       boolean isOaidTrackLimited();
}

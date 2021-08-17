SUMMARY = "Zlib QAT_MEM Memory Management Module for Intel Quick Assist \
Technology"

DESCRIPTION = "This software acelerates the data compression algorithm \
in the zlib software library via the Intel QuickAssist Technology"

HOMEPAGE = "http://zlib.net/"
SECTION = "libs"
LICENSE = "Zlib & GPLv2 & BSD"
LIC_FILES_CHKSUM = "file://${WORKDIR}/zlib-${ZLIB_VERSION}/zlib.h;beginline=4;endline=23;md5=fde612df1e5933c428b73844a0c494fd \
		    file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6 \
		    file://${COMMON_LICENSE_DIR}/BSD;md5=3775480a712fc46a69647678acb234cb"

# For target side versions of openssl enable support for OCF Linux driver
# if they are available.
DEPENDS += "cryptodev-linux pkgconfig udev"

SRC_URI = "http://www.zlib.net/zlib-${ZLIB_VERSION}.tar.gz;name=zlib \
           "
SRC_URI:append:libc-musl = " file://0001-qat_zlib.h-Add-pthread.h-for-MUSL.patch"

SRC_URI[zlib.md5sum] = "44d667c142d7cda120332623eab69f40"
SRC_URI[zlib.sha256sum] = "36658cb768a54c1d4dec43c3116c27ed893e88b02ecfcb44f2166f9c0b7f2a0d"

COMPATIBLE_MACHINE = "null"

ZLIB_VERSION = "1.2.8"

S = "${WORKDIR}/zlib-${ZLIB_VERSION}"

export ICP_ROOT = "${S}"
export ZLIB_ROOT = "${S}"
export KERNEL_SOURCE_ROOT = "${STAGING_KERNEL_DIR}"
export KERNEL_BUILDDIR = "${STAGING_KERNEL_BUILDDIR}"
export ICP_LAC_API_DIR = "${STAGING_DIR_TARGET}${includedir}/lac"
export ICP_DC_API_DIR = "${STAGING_DIR_TARGET}${includedir}/dc"
export ICP_BUILD_OUTPUT = "${STAGING_DIR_TARGET}"

EXTRA_OEMAKE = "-e MAKEFLAGS="
TARGET_CC_ARCH += "${LDFLAGS}"

MEM_PATH = "${S}/contrib/qat"

# We invoke base do_patch at end, to incorporate any local patch
python do_patch() {
    bb.build.exec_func('zlibqat_do_patch', d)
    bb.build.exec_func('patch_do_patch', d)
}

do_configure() {
        ./configure --prefix=${prefix} --shared --libdir=${libdir}
}

do_compile() {
        unset CFLAGS CXXFLAGS
        oe_runmake

        cd ${S}/contrib/qat/qat_zlib_test 
        oe_runmake
}

do_install() {
        install -m 0755 -d		${D}${bindir}/
        install -m 0755 -d		${D}${sysconfdir}/zlib_conf/

        install -m 0755 zpipe ${D}${bindir}
        install -m 0755 minigzip ${D}${bindir}

        cd ${S}/contrib/qat/qat_zlib_test
        oe_runmake DESTDIR=${D} install
}

PACKAGE_BEFORE_PN = "${PN}-app"

FILES:${PN} += " \
		${sysconfdir}/zlib_conf/ \
		"

FILES:${PN}-app += " \
		${bindir}/* \
	"

FILES:${PN}-dbg += " \
		${bindir}/.debug \
		"

EXCLUDE_FROM_WORLD:core2-32-intel-common = "1"

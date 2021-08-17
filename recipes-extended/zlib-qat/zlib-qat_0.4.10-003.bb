require zlib-qat.inc

DEPENDS += "qat17"

SRC_URI += "https://01.org/sites/default/files/downloads/zlibshim0.4.10-003.tar.gz;name=zlibqat \
           file://zlib-qat-0.4.10-001-zlib-qat-add-a-install-target-to-makefile.patch \
           file://zlib-qat-0.4.10-001-zlib-Remove-rpaths-from-makefile.patch \
           file://zlib-qat-0.4.10-001-zlib-qat-correct-the-order-for-static-linking-libude.patch \
           "

SRC_URI[zlibqat.md5sum] = "cea60976411bcb8378adbe1007f7fa6a"
SRC_URI[zlibqat.sha256sum] = "b01f40e2df1a456435972ba46812a23f898e4a592b17ed76240f836b5b992bbd"

ZLIB_QAT_VERSION = "0.4.10-003"

export ZLIB_MEMORY_DRIVER = "usdm_drv"
export CMN_ROOT = "${STAGING_DIR_TARGET}${base_libdir}"
export UPSTREAM_DRIVER_CMN_ROOT = "${STAGING_DIR_TARGET}${base_libdir}"

zlibqat_do_patch() {
        cd ${WORKDIR}
        tar -xvzf zlib-${ZLIB_VERSION}-qat.L.${ZLIB_QAT_VERSION}.tar.gz
        cd ${S}
        if [ ! -d ${S}/debian/patches ]; then
                mkdir -p ${S}/debian/patches
                cp -f ${WORKDIR}/zlib-${ZLIB_VERSION}-qat.patch ${S}/debian/patches
                echo "zlib-${ZLIB_VERSION}-qat.patch -p1" > ${S}/debian/patches/series
        fi
        quilt pop -a || true
        if [ -d ${S}/.pc-zlibqat ]; then
                rm -rf ${S}/.pc
                mv ${S}/.pc-zlibqat ${S}/.pc
                QUILT_PATCHES=${S}/debian/patches quilt pop -a
                rm -rf ${S}/.pc
        fi
        QUILT_PATCHES=${S}/debian/patches quilt push -a
        mv ${S}/.pc ${S}/.pc-zlibqat
}

do_install:append() {
        install -m 660  ${MEM_PATH}/config/c3xxx/multi_thread_optimized/*	${D}${sysconfdir}/zlib_conf/
        install -m 660  ${MEM_PATH}/config/c6xx/multi_thread_optimized/*	${D}${sysconfdir}/zlib_conf/
}

require zlib-qat.inc

DEPENDS += "qat16"

SRC_URI += "https://01.org/sites/default/files/page/zlib_shim_0.4.7-002_withdocumentation.zip;name=zlibqat \
           file://zlib-qat-0.4.7-002-qat_mem-build-qat_mem-ko-against-yocto-kernel-src.patch \
           file://zlib-qat-0.4.7-002-zlib-qat-add-a-install-target-to-makefile.patch \
           file://zlib-qat-0.4.7-002-zlib-Remove-rpaths-from-makefile.patch \
           "

SRC_URI[zlibqat.md5sum] = "dfde8618198aa8d35ecc00d10dcc7000"
SRC_URI[zlibqat.sha256sum] = "8e5786400bbc2a879ae705c864ec63b53ae019b4f2d1c94524a97223847b6e46"

ZLIB_QAT_VERSION = "0.4.7-002"
QAT_PATCH_VERSION = "l.0.4.7_002"

export ZLIB_DH895XCC = "1"
export ZLIB_MEMORY_DRIVER = "qat_mem"

# qat_mem OOT kernel module, thus inherit module.bbclass
inherit module

zlibqat_do_patch() {
        cd ${WORKDIR}
        unzip -q -o zlib_quickassist_patch_${QAT_PATCH_VERSION}_stable.zip
        cd zlib_quickassist_patch_${QAT_PATCH_VERSION}_devbranch
        tar -xvzf zlib-${ZLIB_VERSION}-qat.L.${ZLIB_QAT_VERSION}.tar.gz
        cp -f zlib-${ZLIB_VERSION}-qat.patch ${WORKDIR}
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

# do_compile will override the module_do_compile from inherited module.bbclass
# which causes issues for components other than qat_mem.ko
do_compile() {
        unset CFLAGS CXXFLAGS
        oe_runmake

        cd ${S}/contrib/qat/qat_zlib_test
        oe_runmake

        cd ${S}/contrib/qat/qat_mem
        oe_runmake
}

# do_install will override the module_do_install inherited from module.bbclass
# which causes issues for components other than qat_mem.ko
do_install() {
        install -m 0755 -d		${D}${bindir}/
        install -m 0755 -d		${D}${sysconfdir}/zlib_conf/

        install -m 0755 zpipe ${D}${bindir}
        install -m 0755 minigzip ${D}${bindir}

        cd ${S}/contrib/qat/qat_zlib_test
        oe_runmake DESTDIR=${D} install

        cd ${MEM_PATH}/qat_mem
        oe_runmake INSTALL_MOD_PATH=${D} INSTALL_MOD_DIR="kernel/drivers" install

        install -m 660  ${MEM_PATH}/config/dh895xcc/multi_thread_optimized/*	${D}${sysconfdir}/zlib_conf/
}

# module.bbclass will reset FILES variable
FILES:${PN} += " \
		${sysconfdir}/zlib_conf/ \
		"

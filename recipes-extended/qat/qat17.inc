DESCRIPTION = "Intel(r) QuickAssist Technology API"
HOMEPAGE = "https://01.org/packet-processing/intel%C2%AE-quickassist-technology-drivers-and-patches"

#Dual BSD and GPLv2 License
LICENSE = "BSD & GPLv2"
LIC_FILES_CHKSUM = "\
                    file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6 \
                    file://${COMMON_LICENSE_DIR}/BSD;md5=3775480a712fc46a69647678acb234cb \
                    "
DEPENDS += "boost"
DEPENDS += "udev"
DEPENDS += "zlib openssl"
PROVIDES += "virtual/qat"

SRC_URI="file://qat16_2.3.0-34-qat-fix-for-cross-compilation-issue.patch \
         file://qat16_2.3.0-34-qat-remove-local-path-from-makefile.patch \
         file://qat16_2.3.0-34-make-sure-CFLAGS-are-correct.patch \
         "
#https://01.org/sites/default/files/page/qatmux.l.${PV}.tgz;name=qat
COMPATIBLE_MACHINE = "crystalforest|intel-corei7-64|intel-core2-32"

S = "${WORKDIR}"
ICP_TOOLS = "accelcomp"
SAMPLE_CODE_DIR = "${S}/quickassist/lookaside/access_layer/src/sample_code"
export INSTALL_MOD_PATH = "${D}"
export ICP_DRIVER_TYPE = "QAT1.7"
export ICP_FIRMWARE_DIR="c3xxx"
export ICP_ROOT = "${S}"
export ICP_ENV_DIR = "${S}/quickassist/build_system/build_files/env_files"
export ICP_BUILDSYSTEM_PATH = "${S}/quickassist/build_system"
export ICP_TOOLS_TARGET = "${ICP_TOOLS}"
export FUNC_PATH = "${ICP_ROOT}/quickassist/lookaside/access_layer/src/sample_code/functional"
export INSTALL_FW_PATH = "${D}${base_libdir}/firmware"
export KERNEL_SOURCE_ROOT = "${STAGING_KERNEL_DIR}"
export ICP_BUILD_OUTPUT = "${D}"
export DEST_LIBDIR = "${libdir}"
export DEST_BINDIR = "${bindir}"
export QAT_KERNEL_VER = "${KERNEL_VERSION}"
export SAMPLE_BUILD_OUTPUT = "${D}"
export MODULE_DIR = "${base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers"
export INSTALL_MOD_DIR = "${D}${base_libdir}/modules/${KERNEL_VERSION}"
export KERNEL_BUILDDIR = "${STAGING_KERNEL_BUILDDIR}"
export SC_EPOLL_DISABLED = "1"
export WITH_UPSTREAM = "1"
export WITH_CMDRV = "1"
export KERNEL_SOURCE_DIR = "${ICP_ROOT}/quickassist/qat/"

export BIN_LIST="qat_c3xxx.bin qat_c3xxx_a0.bin qat_c3xxx_mmp.bin qat_c62x.bin qat_c62x_mmp.bin"
export BIN_DH895XCC="qat_895xcc.bin qat_mmp.bin"
export BIN_C62X="qat_c62x.bin qat_c62x_mmp.bin"
export BIN_C3XXX="qat_c3xxx.bin qat_c3xxx_mmp.bin"

export KO_INTEL_QAT="${S}/quickassist/qat/drivers/crypto/qat/qat_common"
export KO_QAT_DH895XCC="${S}/quickassist/qat/drivers/crypto/qat/qat_dh895xcc"
export KO_QAT_DH895XCCVF="${S}/quickassist/qat/drivers/crypto/qat/qat_dh895xccvf"
export KO_QAT_C62X="${S}/quickassist/qat/drivers/crypto/qat/qat_c62x"
export KO_QAT_C62XVF="${S}/quickassist/qat/drivers/crypto/qat/qat_c62xvf"
export KO_QAT_C3XXX="${S}/quickassist/qat/drivers/crypto/qat/qat_c3xxx"
export KO_QAT_C3XXXVF="${S}/quickassist/qat/drivers/crypto/qat/qat_c3xxxvf"

inherit module
inherit update-rc.d
INITSCRIPT_NAME = "qat_service"

PARALLEL_MAKE = ""

#To get around the double slashes in paths in QAT makefiles
PACKAGE_DEBUG_SPLIT_STYLE = "debug-without-src"

EXTRA_OEMAKE_append = " CFLAGS+='-fgnu89-inline -fPIC'"
EXTRA_OEMAKE = "-e MAKEFLAGS="

do_compile () {
	export LD="${LD} --hash-style=gnu"
	export MACHINE="${TARGET_ARCH}"

        cd ${S}/quickassist
        oe_runmake

	cd ${S}/quickassist/qat
	oe_runmake 'clean'
	oe_runmake 'modules_install'

	cd ${S}/quickassist/utilities/adf_ctl
	oe_runmake

	cd ${S}/quickassist/utilities/libusdm_drv
        oe_runmake

	#build the whole sample code: per_user only
	cd ${SAMPLE_CODE_DIR}
	oe_runmake 'perf_user'
}

do_install() {
	cp ${TOPDIR}/downloads/qat1.7.upstream.l.${PV}.tar.gz ${S}
	export MACHINE="${TARGET_ARCH}"

	cd ${S}/quickassist
        oe_runmake install

        install -d ${D}/etc/udev/rules.d
	install -d ${D}${sbindir}
	install -d ${D}${sysconfdir}/conf_files
	install -d ${D}${prefix}/qat

	echo 'KERNEL=="icp_adf_ctl" MODE="0600"' > ${D}/etc/udev/rules.d/00-dh895xcc_qa.rules
	echo 'KERNEL=="icp_dev[0-9]*" MODE="0600"' >> ${D}/etc/udev/rules.d/00-dh895xcc_qa.rules
	echo 'KERNEL=="icp_dev_mem?" MODE="0600"' >> ${D}/etc/udev/rules.d/00-dh895xcc_qa.rules

	install -m 0755 ${S}/qat1.7.upstream.l.${PV}.tar.gz ${D}${prefix}/qat

	install -D -m 0755 ${S}/quickassist/lookaside/access_layer/src/build/linux_2.6/user_space/*.so ${D}${base_libdir}
	install -D -m 0755 ${S}/quickassist/utilities/libusdm_drv/libusdm_drv_s.so ${D}${base_libdir}
	install -D -m 0750 ${S}/quickassist/utilities/adf_ctl/adf_ctl ${D}${sbindir}
	install -D -m 640 ${S}/quickassist/utilities/adf_ctl/conf_files/c3xxx_dev0.conf  ${D}${sysconfdir}
	install -D -m 640 ${S}/quickassist/utilities/adf_ctl/conf_files/c3xxx_dev0.conf  ${D}${sysconfdir}/conf_files
	install -D -m 640 ${S}/quickassist/utilities/adf_ctl/conf_files/c3xxxvf_dev0.conf.vm  ${D}${sysconfdir}/conf_files
	install -m 0755 ${S}/quickassist/qat/fw/qat_c3xxx.bin  ${D}${base_libdir}/firmware
	install -m 0755 ${S}/quickassist/qat/fw/qat_c3xxx_a0.bin  ${D}${base_libdir}/firmware
	install -m 0755 ${S}/quickassist/qat/fw/qat_c3xxx_mmp.bin  ${D}${base_libdir}/firmware
	install -m 0755 ${S}/quickassist/qat/fw/qat_c62x.bin  ${D}${base_libdir}/firmware
	install -m 0755 ${S}/quickassist/qat/fw/qat_c62x_mmp.bin  ${D}${base_libdir}/firmware
	install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/calgary  ${D}${base_libdir}/firmware
	install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/calgary32  ${D}${base_libdir}/firmware
	install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/canterbury  ${D}${base_libdir}/firmware
}

PACKAGES += "${PN}-app"

FILES_${PN}-dev = "${includedir}/ \
                  "
FILES_${PN} += "\
                ${libdir}/ \
                ${base_libdir}/firmware \
                ${sysconfdir}/ \
		${sbindir}/ \
		${base_libdir}/*.so \
                "

FILES_${PN}-dbg += "${sysconfdir}/init.d/.debug/ \
		   "

FILES_${PN}-app += "${bindir}/* \
		    ${prefix}/qat \
		   "

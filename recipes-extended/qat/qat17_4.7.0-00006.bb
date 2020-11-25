DESCRIPTION = "Intel(r) QuickAssist Technology API"
HOMEPAGE = "https://01.org/packet-processing/intel%C2%AE-quickassist-technology-drivers-and-patches"

#Dual BSD and GPLv2 License
LICENSE = "BSD & GPLv2"
LIC_FILES_CHKSUM = "\
                    file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6 \
                    file://${COMMON_LICENSE_DIR}/BSD;md5=3775480a712fc46a69647678acb234cb \
                    "
DEPENDS += "boost udev zlib openssl"
PROVIDES += "virtual/qat"

TARGET_CC_ARCH += "${LDFLAGS}"

SRC_URI = "https://01.org/sites/default/files/downloads/qat1.7.l.4.7.0-00006.tar.gz;subdir=qat17 \
           file://qat16_2.3.0-34-qat-remove-local-path-from-makefile.patch \
           file://qat16_2.6.0-65-qat-override-CC-LD-AR-only-when-it-is-not-define.patch \
           file://qat17_0.6.0-1-qat-update-KDIR-for-cross-compilation.patch \
           file://qat17_0.8.0-37-qat-added-include-dir-path.patch \
           file://qat17_0.9.0-4-qat-add-install-target-and-add-folder.patch \
           file://qat17_4.1.0-00022-qat-use-static-lib-for-linking.patch \
           file://qat17_4.7.0-00006-Link-driver-with-object-files.patch \
           file://qat17_4.7.0-00006-Drop-pr_warning-definition.patch \
           file://qat17_4.7.0-00006-Switch-to-skcipher-API.patch;apply=0 \
           file://qat17_4.7.0-00006-make-it-compatible-with-kernel-5.6.patch;apply=0 \
           file://qat17_4.7.0-00006-crypto-qat-adf_aer-Remove-pci_cleanup_aer_uncorrect_.patch \
           file://qat17_4.7.0-00006-qat-replace-linux-cryptohash.h-with-crypto-sha.h-for.patch \
          "

do_fetch[depends] += "virtual/kernel:do_shared_workdir"

do_patch_append () {
    kernel_version = int(d.getVar("KERNEL_VERSION").split(".")[0])
    kernel_patchlevel = int(d.getVar("KERNEL_VERSION").split(".")[1])

    if kernel_version >= 5 and kernel_patchlevel >= 5:
        bb.build.exec_func('do_switch_to_skcipher_api', d)
    if kernel_version >= 5 and kernel_patchlevel >= 6:
        bb.build.exec_func('do_patch_for_kernel_5_6', d)
}

do_switch_to_skcipher_api () {
    cd "${S}"
    patch -p1 < "${WORKDIR}/qat17_4.7.0-00006-Switch-to-skcipher-API.patch"
}

do_patch_for_kernel_5_6 () {
    cd "${S}"
    patch -p1 < "${WORKDIR}/qat17_4.7.0-00006-make-it-compatible-with-kernel-5.6.patch"
}


SRC_URI[md5sum] = "ac939b51cc8836c182e31e309c065002"
SRC_URI[sha256sum] = "5c8bdc35fd7a42f212f1f87eb9e3d8584df7af56dae366debc487981e531fa5c"

COMPATIBLE_MACHINE = "null"
COMPATIBLE_HOST_x86-x32 = 'null'
COMPATIBLE_HOST_libc-musl_class-target = 'null'

S = "${WORKDIR}/qat17"
ICP_TOOLS = "accelcomp"
SAMPLE_CODE_DIR = "${S}/quickassist/lookaside/access_layer/src/sample_code"
export INSTALL_MOD_PATH = "${D}"
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
export INSTALL_MOD_DIR = "${D}${base_libdir}/modules/${KERNEL_VERSION}"
export KERNEL_BUILDDIR = "${STAGING_KERNEL_BUILDDIR}"
export SC_EPOLL_DISABLED = "1"
export WITH_UPSTREAM = "1"
export WITH_CMDRV = "1"
export KERNEL_SOURCE_DIR = "${ICP_ROOT}/quickassist/qat/"
export ICP_NO_CLEAN = "1"

inherit module
inherit update-rc.d
INITSCRIPT_NAME = "qat_service"

PARALLEL_MAKE = ""

EXTRA_OEMAKE_append = " CFLAGS+='-fgnu89-inline -fPIC'"
EXTRA_OEMAKE = "-e MAKEFLAGS="

do_compile () {
  export LD="${LD} --hash-style=gnu"
  export MACHINE="${TARGET_ARCH}"

  cd ${S}/quickassist/qat
  oe_runmake
  oe_runmake 'modules_install'

  cd ${S}/quickassist
  oe_runmake

  cd ${S}/quickassist/utilities/adf_ctl
  oe_runmake

  cd ${S}/quickassist/utilities/libusdm_drv
  oe_runmake

  cd ${S}/quickassist/lookaside/access_layer/src/qat_direct/src/
  oe_runmake

  #build the whole sample code: per_user only
  cd ${SAMPLE_CODE_DIR}
  oe_runmake 'perf_user'
}

do_install() {
  export MACHINE="${TARGET_ARCH}"

  cd ${S}/quickassist
  oe_runmake install

  cd ${S}/quickassist/qat
  oe_runmake modules_install

  install -d ${D}${sysconfdir}/udev/rules.d
  install -d ${D}${sbindir}
  install -d ${D}${sysconfdir}/conf_files
  install -d ${D}${prefix}/src/qat
  install -d ${D}${includedir}
  install -d ${D}${includedir}/dc
  install -d ${D}${includedir}/lac

  echo 'KERNEL=="qat_adf_ctl" MODE="0660" GROUP="qat"' > ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="qat_dev_processes" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="usdm_drv" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="uio*" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules
  echo 'KERNEL=="hugepages" MODE="0660" GROUP="qat"' >> ${D}/etc/udev/rules.d/00-qat.rules

  mkdir -p ${D}${base_libdir}

  install -D -m 0755 ${S}/quickassist/lookaside/access_layer/src/build/linux_2.6/user_space/libqat_s.so ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/lookaside/access_layer/src/build/linux_2.6/user_space/libqat.a ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/utilities/osal/src/build/linux_2.6/user_space/libosal_s.so ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/utilities/osal/src/build/linux_2.6/user_space/libosal.a ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/lookaside/access_layer/src/qat_direct/src/build/linux_2.6/user_space/libadf_user.a ${D}${base_libdir}/libadf.a
  install -D -m 0755 ${S}/quickassist/utilities/libusdm_drv/libusdm_drv_s.so ${D}${base_libdir}
  install -D -m 0755 ${S}/quickassist/utilities/libusdm_drv/libusdm_drv.a ${D}${base_libdir}
  install -D -m 0750 ${S}/quickassist/utilities/adf_ctl/adf_ctl ${D}${sbindir}

  install -D -m 640 ${S}/quickassist/utilities/adf_ctl/conf_files/*.conf  ${D}${sysconfdir}/conf_files
  install -D -m 640 ${S}/quickassist/utilities/adf_ctl/conf_files/*.conf.vm  ${D}${sysconfdir}/conf_files

  install -m 0755 ${S}/quickassist/qat/fw/qat_d15xx.bin  ${D}${nonarch_base_libdir}/firmware
  install -m 0755 ${S}/quickassist/qat/fw/qat_d15xx_mmp.bin  ${D}${nonarch_base_libdir}/firmware

  install -m 640 ${S}/quickassist/include/*.h  ${D}${includedir}
  install -m 640 ${S}/quickassist/include/dc/*.h  ${D}${includedir}/dc/
  install -m 640 ${S}/quickassist/include/lac/*.h  ${D}${includedir}/lac/
  install -m 640 ${S}/quickassist/lookaside/access_layer/include/*.h  ${D}${includedir}
  install -m 640 ${S}/quickassist/utilities/libusdm_drv/*.h  ${D}${includedir}

  install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/calgary  ${D}${nonarch_base_libdir}/firmware
  install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/calgary32  ${D}${nonarch_base_libdir}/firmware
  install -m 0755 ${S}/quickassist/lookaside/access_layer/src/sample_code/performance/compression/canterbury  ${D}${nonarch_base_libdir}/firmware

  #install qat source
  cp ${DL_DIR}/qat1.7.l.${PV}.tar.gz ${D}${prefix}/src/qat/
}

PACKAGES += "${PN}-app"

FILES_${PN}-dev = "${includedir}/ \
                   ${nonarch_base_libdir}/*.a \
                   "

FILES_${PN} += "\
                ${libdir}/ \
                ${nonarch_base_libdir}/firmware \
                ${sysconfdir}/ \
                ${sbindir}/ \
                ${base_libdir}/*.so \
                ${prefix}/src/qat \
                "

FILES_${PN}-dbg += "${sysconfdir}/init.d/.debug/ \
                    "

FILES_${PN}-app += "${bindir}/* \
                    ${prefix}/qat \
                    "

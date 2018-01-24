include qat17.inc

DESCRIPTION = "Intel(r) QuickAssist Technology API"
HOMEPAGE = "https://01.org/packet-processing/intel%C2%AE-quickassist-technology-drivers-and-patches"

#Dual BSD and GPLv2 License
LICENSE = "BSD & GPLv2"

TARGET_CC_ARCH += "${LDFLAGS}"

SRC_URI="https://01.org/sites/default/files/downloads/intelr-quickassist-technology/qat1.7.upstream.l.1.0.3-42.tar.gz \
         file://qat16_2.3.0-34-qat-remove-local-path-from-makefile.patch \
         file://qat16_2.6.0-65-qat-override-CC-LD-AR-only-when-it-is-not-define.patch \
         file://qat17_0.6.0-1-qat-fix-kernel-patch.patch \
         file://qat17_0.8.0-37-qat-added-include-dir-path.patch \
         file://qat17_0.9.0-4-qat-add-install-target-and-add-folder.patch \
         "

SRC_URI[md5sum] = "ee059cf134486f5684466555e8636ee0"
SRC_URI[sha256sum] = "84d2c4a242987548135274da7c3def31461af6f1b4beb74f519a993e854abf5b"

# Support for 4.14 not yet available
python () {
    if d.getVar("PREFERRED_PROVIDER_virtual/kernel") == "linux-intel" and \
       d.getVar("PREFERRED_VERSION_linux-intel") == "4.14%" or \
       d.getVar("PREFERRED_PROVIDER_virtual/kernel") == "linux-intel-rt" and \
       d.getVar("PREFERRED_VERSION_linux-intel-rt") == "4.14%":
        raise bb.parse.SkipPackage("This version of QAT has not been tested with Linux Kernel 4.14 or newer")
}

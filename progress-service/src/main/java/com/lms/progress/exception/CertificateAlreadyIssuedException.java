package com.lms.progress.exception;

/** Thrown when issueCertificate is called but a certificate already exists for that student/course. */
public class CertificateAlreadyIssuedException extends RuntimeException {
    public CertificateAlreadyIssuedException(String message) {
        super(message);
    }
}

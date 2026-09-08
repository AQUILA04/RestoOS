package com.resto.core.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    private HttpStatus status;
    private int statusCode;
    private String message;
    private String service;
    private T data;

    public HttpStatus getStatus() { return status; }
    public void setStatus(HttpStatus status) { this.status = status; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> ResponseBuilder<T> builder() {
        return new ResponseBuilder<T>().service("RESTO-OS");
    }

    public static class ResponseBuilder<T> {
        private HttpStatus status;
        private int statusCode;
        private String message;
        private String service = "RESTO-OS";
        private T data;

        public ResponseBuilder<T> status(HttpStatus status) { this.status = status; return this; }
        public ResponseBuilder<T> statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public ResponseBuilder<T> message(String message) { this.message = message; return this; }
        public ResponseBuilder<T> service(String service) { this.service = service; return this; }
        public ResponseBuilder<T> data(T data) { this.data = data; return this; }

        public Response<T> build() {
            Response<T> resp = new Response<>();
            resp.setStatus(this.status);
            resp.setStatusCode(this.statusCode);
            resp.setMessage(this.message);
            resp.setService(this.service);
            resp.setData(this.data);
            return resp;
        }
    }
}

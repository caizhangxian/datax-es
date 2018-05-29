package com.alibaba.datax.plugin.writer.elasticsearchwriter;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * @Description:
 * @Author:caizhangxian
 * @Date:2018/5/24
 */
public class LibraryBooksEntity extends EsEntity {
    private Integer id;
    private String isbn;
    private Double price;
    private Integer bookId;
    private String storeRoom;
    private String isEffective;
    private String barNumber;
    private String frameCode;
    private Integer customerId;
    private String  isChecked;
    private String checkedDate;
    private Integer operUser;
    private String bookName;
    private String author;
    private String publisher;
    private String publishDate;
    private String storageTime;
    private String categoryCode;
    private String catalogueDate;
    private String bookState;
    private Integer borrowNum;
    private Integer praiseNum;
    private Integer canAppoint;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date solrUpdate;
    private String libCode;
    private String belongLibraryHallCode;
    private String callNumber;


    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public String getStoreRoom() {
        return storeRoom;
    }

    public void setStoreRoom(String storeRoom) {
        this.storeRoom = storeRoom;
    }

    public String getIsEffective() {
        return isEffective;
    }

    public void setIsEffective(String isEffective) {
        this.isEffective = isEffective;
    }

    public String getBarNumber() {
        return barNumber;
    }

    public void setBarNumber(String barNumber) {
        this.barNumber = barNumber;
    }

    public String getFrameCode() {
        return frameCode;
    }

    public void setFrameCode(String frameCode) {
        this.frameCode = frameCode;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(String isChecked) {
        this.isChecked = isChecked;
    }

    public String getCheckedDate() {
        return checkedDate;
    }

    public void setCheckedDate(String checkedDate) {
        this.checkedDate = checkedDate;
    }

    public Integer getOperUser() {
        return operUser;
    }

    public void setOperUser(Integer operUser) {
        this.operUser = operUser;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getStorageTime() {
        return storageTime;
    }

    public void setStorageTime(String storageTime) {
        this.storageTime = storageTime;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCatalogueDate() {
        return catalogueDate;
    }

    public void setCatalogueDate(String catalogueDate) {
        this.catalogueDate = catalogueDate;
    }

    public String getBookState() {
        return bookState;
    }

    public void setBookState(String bookState) {
        this.bookState = bookState;
    }

    public Integer getBorrowNum() {
        return borrowNum;
    }

    public void setBorrowNum(Integer borrowNum) {
        this.borrowNum = borrowNum;
    }

    public Integer getPraiseNum() {
        return praiseNum;
    }

    public void setPraiseNum(Integer praiseNum) {
        this.praiseNum = praiseNum;
    }

    public Integer getCanAppoint() {
        return canAppoint;
    }

    public void setCanAppoint(Integer canAppoint) {
        this.canAppoint = canAppoint;
    }

    public Date getSolrUpdate() {
        return solrUpdate;
    }

    public void setSolrUpdate(Date solrUpdate) {
        this.solrUpdate = solrUpdate;
    }

    public String getLibCode() {
        return libCode;
    }

    public void setLibCode(String libCode) {
        this.libCode = libCode;
    }

    public String getBelongLibraryHallCode() {
        return belongLibraryHallCode;
    }

    public void setBelongLibraryHallCode(String belongLibraryHallCode) {
        this.belongLibraryHallCode = belongLibraryHallCode;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }


}

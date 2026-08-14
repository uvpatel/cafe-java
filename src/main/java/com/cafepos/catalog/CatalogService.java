package com.cafepos.catalog;

import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    public String getCatalogStatus() {
        return "Catalog Active";
    }
}

package com.example.util.jasperreport

/**
 * 建立 [SimpleBean] 測試資料清單的工廠。
 */
class SimpleBeanMaker {

    /**
     * 產生固定的人物資料清單，供 JasperReports 報表測試使用。
     *
     * @return [SimpleBean] 清單
     */
    fun getDataBeanList(): ArrayList<SimpleBean> {
        val dataBeanList = ArrayList<SimpleBean>()

        // dataBeanList.add(produce("Babji, Chetty", "Engineer", "Nuremberg", "Germany"))
        dataBeanList.add(produce("Albert Einstein", "Engineer", "Ulm", "Germany"))
        dataBeanList.add(produce("Alfred Hitchcock", "Movie Director", "London", "UK"))
        dataBeanList.add(produce("Wernher Von Braun", "Rocket Scientist", "Wyrzysk", "Poland (Previously Germany)"))
        dataBeanList.add(produce("Sigmund Freud", "Neurologist", "Pribor", "Czech Republic (Previously Austria)"))
        dataBeanList.add(produce("Mahatma Gandhi", "Lawyer", "Gujarat", "India"))
        dataBeanList.add(produce("Sachin Tendulkar", "Cricket Player", "Mumbai", "India"))
        dataBeanList.add(produce("Michael Schumacher", "F1 Racer", "Cologne", "Germany"))

        return dataBeanList
    }

    private fun produce(name: String, occupation: String, place: String, country: String): SimpleBean {
        val dataBean = SimpleBean()
        dataBean.name = name
        dataBean.occupation = occupation
        dataBean.place = place
        dataBean.country = country
        return dataBean
    }
}
